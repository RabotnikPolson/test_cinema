import pandas as pd
import numpy as np
from sqlalchemy import create_engine, text
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from database import DATABASE_URL
from functools import lru_cache

engine = create_engine(DATABASE_URL)
import time

KAZAKHSTAN_BOOST = 1.4
CENTRAL_ASIA_BOOST = 1.3
CIS_BOOST = 1.3
DEFAULT_BOOST = 1.0

def get_kazakhstan_boost_score(movie_row) -> float:
    country = str(movie_row.get("country", "")) if 'country' in movie_row.index and pd.notna(movie_row['country']) else ""
    country = country.lower()
    language = str(movie_row.get("language", "")) if 'language' in movie_row.index and pd.notna(movie_row['language']) else ""
    language = language.lower()
    
    if "казахстан" in country or "kazakhstan" in country or "kz" in country:
        return KAZAKHSTAN_BOOST
    if "казахский" in language or "kazakh" in language or "қазақ" in language:
        return KAZAKHSTAN_BOOST
    if any(c in country for c in ["кыргыз", "узбек", "таджик", "туркмен"]):
        return CENTRAL_ASIA_BOOST
    if any(c in country for c in ["россия", "russia", "беларусь", "украина", "азербайджан"]):
        return CIS_BOOST
    return DEFAULT_BOOST

def get_recommendations_model():

    try:
        query = "SELECT id, title, genre_text, description, imdb_rating, director, actors, is_domestic, poster_url, year, country, language FROM movies"
        df = pd.read_sql(query, engine)
    except Exception as e:
        print(f"Warning: {e}. Falling back to old schema.")
        query = "SELECT id, title, genre_text, description, imdb_rating FROM movies"
        df = pd.read_sql(query, engine)
        df['director'] = ''
        df['actors'] = ''
        df['is_domestic'] = False
        df['poster_url'] = ''
        df['year'] = ''
        
    df['director_clean'] = df['director'].fillna('').astype(str)
    df['actors_clean'] = df['actors'].fillna('').astype(str)
    df['genre_clean'] = df['genre_text'].fillna('').astype(str)
    df['title_clean'] = df['title'].fillna('').astype(str)
    df['is_domestic'] = df['is_domestic'].fillna(False).astype(bool)

        
    # Weighted TF-IDF: genre и director повторяем для большего веса
    df['rating_num'] = pd.to_numeric(df['imdb_rating'], errors='coerce').fillna(0)
    r_min = df['rating_num'].min()
    r_max = df['rating_num'].max()
    df['rating_norm'] = (df['rating_num'] - r_min) / max(r_max - r_min, 1e-8)

    df['content'] = (
        df['genre_clean'] + ' ' + df['genre_clean'] + ' ' + df['genre_clean'] + ' ' +
        df['director_clean'] + ' ' + df['director_clean'] + ' ' +
        df['description'].fillna('') + ' ' +
        df['actors_clean']
    )

    tfidf = TfidfVectorizer(stop_words='english', ngram_range=(1, 2), max_features=8000)
    tfidf_matrix = tfidf.fit_transform(df['content'])
    cosine_sim = cosine_similarity(tfidf_matrix, tfidf_matrix)
    return df, cosine_sim

def _safe_year(year_val):
    """Safely convert year value to int or None."""
    try:
        if year_val is None:
            return None
        if pd.isna(year_val):
            return None
        s = str(year_val).strip()
        if not s or s == 'nan' or s == 'None':
            return None
        return int(float(s))
    except (ValueError, TypeError):
        return None

def _format_recs(df, indices, scores=None, reasons=None):
    recs = []
    for i, idx in enumerate(indices):
        score = scores[i] if scores is not None else 0.0
        reason = reasons[i] if reasons is not None else ""
        year_val = df['year'].iloc[idx] if 'year' in df.columns else None
        poster_val = df['poster_url'].iloc[idx] if 'poster_url' in df.columns else None
        recs.append({
            "movie_id": int(df['id'].iloc[idx]),
            "score": round(float(score), 3),
            "title": df['title'].iloc[idx],
            "poster_url": str(poster_val) if poster_val is not None and pd.notna(poster_val) else None,
            "year": _safe_year(year_val),
            "reason": reason
        })
    return recs

def _is_kz_film(df, idx: int) -> bool:
    country = str(df['country'].iloc[idx]).lower() if 'country' in df.columns and pd.notna(df['country'].iloc[idx]) else ''
    language = str(df['language'].iloc[idx]).lower() if 'language' in df.columns and pd.notna(df['language'].iloc[idx]) else ''
    return ('казахстан' in country or 'kazakhstan' in country or
            'казахский' in language or 'kazakh' in language or 'қазақ' in language)


def _apply_kz_ratio(df, indices, scores, reasons, top_n, max_kz_ratio=0.35):
    """Cap KZ films at max_kz_ratio of results (default 35%). Gathers top_n * 3 candidates first."""
    max_kz = max(1, round(top_n * max_kz_ratio))
    kz, foreign = [], []
    for idx, score, reason in zip(indices, scores, reasons):
        if _is_kz_film(df, int(idx)):
            kz.append((idx, score, reason))
        else:
            foreign.append((idx, score, reason))
    selected = kz[:max_kz] + foreign[:top_n - min(len(kz), max_kz)]
    selected.sort(key=lambda x: float(x[1]), reverse=True)
    selected = selected[:top_n]
    if not selected:
        return list(indices[:top_n]), list(scores[:top_n]), list(reasons[:top_n])
    out_i, out_s, out_r = zip(*selected)
    return list(out_i), list(out_s), list(out_r)


def get_popular_fallback(df, top_n=5):
    df['rating_num'] = pd.to_numeric(df['imdb_rating'], errors='coerce').fillna(0)
    popular = df.sort_values(by='rating_num', ascending=False).head(top_n)
    result = []
    for _, r in popular.iterrows():
        poster = r['poster_url'] if 'poster_url' in r.index and pd.notna(r['poster_url']) else None
        year = _safe_year(r['year']) if 'year' in r.index else None
        result.append({"movie_id": int(r['id']), "score": float(r['rating_num']), "title": r['title'], "poster_url": poster, "year": year})
    return result

def get_franchise_recommendations(movie_id, df, top_n=5):
    if movie_id not in df['id'].values: return []
    target_idx = df.index[df['id'] == movie_id][0]
    target_title = df['title_clean'].iloc[target_idx]
    
    prefix = target_title.split(":")[0].strip().lower() if ":" in target_title else " ".join(target_title.split(" ")[:2]).lower()
    if not prefix: return []
    
    scores = []
    reasons = []
    for i in range(len(df)):
        if i == target_idx:
            scores.append(0.0)
            reasons.append("")
            continue
        title = df['title_clean'].iloc[i].lower()
        if title.startswith(prefix) or prefix in title:
            scores.append(1.0)
            reasons.append("Та же франшиза")
        else:
            scores.append(0.0)
            reasons.append("")
            
    scores = np.array(scores)
    top_indices = scores.argsort()[::-1][:top_n]
    top_indices = [idx for idx in top_indices if scores[idx] > 0]
    return _format_recs(df, top_indices, scores[top_indices], [reasons[idx] for idx in top_indices])

def get_director_recommendations(movie_id, df, top_n=5):
    if movie_id not in df['id'].values: return []
    target_idx = df.index[df['id'] == movie_id][0]
    director = df['director_clean'].iloc[target_idx]
    
    if not director or director.strip() == '': return []
    
    matches = df[(df['director_clean'] == director) & (df['id'] != movie_id)]
    if matches.empty: return []
    
    # Sort by rating or just take first N
    matches = matches.copy()
    matches['rating_num'] = pd.to_numeric(matches['imdb_rating'], errors='coerce').fillna(0)
    best = matches.sort_values(by='rating_num', ascending=False).head(top_n)
    
    result = []
    for _, r in best.iterrows():
        poster = r['poster_url'] if 'poster_url' in r.index and pd.notna(r['poster_url']) else None
        year = _safe_year(r['year']) if 'year' in r.index else None
        result.append({"movie_id": int(r['id']), "score": float(r['rating_num']), "title": r['title'], "poster_url": poster, "year": year, "reason": f"Тот же режиссер: {director}"})
    return result

def get_actor_recommendations(movie_id, df, top_n=5):
    if movie_id not in df['id'].values: return []
    target_idx = df.index[df['id'] == movie_id][0]
    actors_str = df['actors_clean'].iloc[target_idx]
    
    if not actors_str or actors_str.strip() == '': return []
    
    target_actors = set([a.strip().lower() for a in actors_str.split(',') if a.strip()])
    if not target_actors: return []
    
    scores = []
    reasons = []
    for i in range(len(df)):
        if i == target_idx:
            scores.append(0.0)
            reasons.append("")
            continue
        other_actors = set([a.strip().lower() for a in df['actors_clean'].iloc[i].split(',') if a.strip()])
        shared = target_actors.intersection(other_actors)
        
        shared_display = [a.strip() for a in df['actors_clean'].iloc[i].split(',') if a.strip().lower() in shared]
        
        scores.append(len(shared) * 0.2) # Weight from prompt
        if shared_display:
            reasons.append("Совпадает актер(ы): " + ", ".join(shared_display))
        else:
            reasons.append("")
        
    scores = np.array(scores)
    top_indices = scores.argsort()[::-1][:top_n]
    top_indices = [idx for idx in top_indices if scores[idx] > 0]
    
    return _format_recs(df, top_indices, scores[top_indices], [reasons[idx] for idx in top_indices])

def get_genre_recommendations(movie_id, df, top_n=5):
    if movie_id not in df['id'].values: return []
    target_idx = df.index[df['id'] == movie_id][0]
    target_genres = set([g.strip().lower() for g in df['genre_clean'].iloc[target_idx].split(',') if g.strip()])
    
    if not target_genres: return []
    
    scores = []
    reasons = []
    for i in range(len(df)):
        if i == target_idx:
            scores.append(0.0)
            reasons.append("")
            continue
        other_genres = set([g.strip().lower() for g in df['genre_clean'].iloc[i].split(',') if g.strip()])
        shared = target_genres.intersection(other_genres)
        scores.append(len(shared) / max(1, len(target_genres))) # Jaccard-like or just ratio
        
        shared_display = [g.strip() for g in df['genre_clean'].iloc[i].split(',') if g.strip().lower() in shared]
        if shared_display:
            reasons.append("Общие жанры: " + ", ".join(shared_display))
        else:
            reasons.append("")
        
    scores = np.array(scores)
    top_indices = scores.argsort()[::-1][:top_n]
    top_indices = [idx for idx in top_indices if scores[idx] > 0]
    return _format_recs(df, top_indices, scores[top_indices], [reasons[idx] for idx in top_indices])

def get_content_recommendations(movie_id, df, cosine_sim, top_n=5):
    if movie_id not in df['id'].values: return []
    idx = df.index[df['id'] == movie_id][0]
    sim_scores = sorted(list(enumerate(cosine_sim[idx])), key=lambda x: x[1], reverse=True)
    sim_scores = sim_scores[1:top_n+1]
    
    indices = [i for i, s in sim_scores]
    scores = [s for i, s in sim_scores]
    reasons = ["Похожий контент" for _ in sim_scores]
    return _format_recs(df, indices, scores, reasons)

def get_hybrid_recommendations(movie_id, df, cosine_sim, top_n=5):
    if movie_id not in df['id'].values: return []
    target_idx = df.index[df['id'] == movie_id][0]
    
    target_title = df['title_clean'].iloc[target_idx]
    prefix = target_title.split(":")[0].strip().lower() if ":" in target_title else " ".join(target_title.split(" ")[:2]).lower()
    
    target_director = df['director_clean'].iloc[target_idx].strip().lower()
    target_actors = set([a.strip().lower() for a in df['actors_clean'].iloc[target_idx].split(',') if a.strip()])
    target_genres = set([g.strip().lower() for g in df['genre_clean'].iloc[target_idx].split(',') if g.strip()])
    
    final_scores = np.zeros(len(df))
    
    # Also fetch collaborative score approximation if possible (expensive here, skipping for simple hybrid, or we can use item-item collab filtering)
    # We will simulate collab as 0.0 in this fast matrix computation if no pre-computed item-item matrix exists
    
    for i in range(len(df)):
        if i == target_idx: continue
        
        movie_title = df['title_clean'].iloc[i].lower()
        movie_director = df['director_clean'].iloc[i].strip().lower()
        movie_actors = set([a.strip().lower() for a in df['actors_clean'].iloc[i].split(',') if a.strip()])
        movie_genres = set([g.strip().lower() for g in df['genre_clean'].iloc[i].split(',') if g.strip()])
        is_domestic = df['is_domestic'].iloc[i]
        
        # 1. Title match
        title_score = 1.0 if (prefix and (movie_title.startswith(prefix) or prefix in movie_title)) else 0.0
        
        # 2. Actor match
        shared_actors = len(target_actors.intersection(movie_actors))
        actor_score = min(1.0, shared_actors * 0.2)
        
        # 3. Director match
        director_score = 1.0 if (target_director and target_director == movie_director) else 0.0
        
        # 4. Genre score
        shared_genres = len(target_genres.intersection(movie_genres))
        genre_score = shared_genres / max(1, len(target_genres)) if target_genres else 0.0
        
        # 5. Content similarity score (TF-IDF)
        content_score = cosine_sim[target_idx][i]
        
        # 6. Collaborative score (mocked as 0 for this function unless fetched)
        collaborative_score = 0.0
        
        # Рейтинг как tie-breaker
        rating_boost = float(df['rating_norm'].iloc[i]) if 'rating_norm' in df.columns else 0.0

        # Исправленные веса: жанр важнее названия
        score = (genre_score     * 0.30 +
                 content_score   * 0.25 +
                 director_score  * 0.20 +
                 actor_score     * 0.15 +
                 rating_boost    * 0.10)

        # Буст для казахского кино (бизнес-требование)
        if is_domestic:
            score += 0.15

        final_scores[i] = score

    top_indices = final_scores.argsort()[::-1][:top_n]
    top_indices = [idx for idx in top_indices if final_scores[idx] > 0]
    reasons = []
    for idx in top_indices:
        parts = []
        if df['genre_clean'].iloc[idx]: parts.append("схожие жанры")
        if df['director_clean'].iloc[idx] == df['director_clean'].iloc[target_idx] and df['director_clean'].iloc[target_idx]: parts.append(f"режиссёр: {df['director_clean'].iloc[target_idx]}")
        if df['is_domestic'].iloc[idx]: parts.append("казахское кино")
        reasons.append("Гибридная рекомендация: " + ", ".join(parts) if parts else "Гибридная рекомендация")
    return _format_recs(df, top_indices, final_scores[top_indices], reasons)

def get_collaborative_users_also_watched(movie_id, df, top_n=5):

    query = text("""
    SELECT movie_id as recommended_movie_id, COUNT(*) as watch_count
    FROM watch_history
    WHERE user_id IN (
       SELECT user_id
       FROM watch_history
       WHERE movie_id = :movie_id
    )
    AND movie_id != :movie_id
    GROUP BY recommended_movie_id
    ORDER BY watch_count DESC
    LIMIT :top_n
    """)
    try:
        df_collab = pd.read_sql(query, engine, params={"movie_id": movie_id, "top_n": top_n})
        if df_collab.empty: return []

        ids = df_collab['recommended_movie_id'].tolist()
        placeholders = ",".join(str(int(i)) for i in ids)
        df_movies = pd.read_sql(
            f"SELECT id, title, poster_url, year, country, language FROM movies WHERE id IN ({placeholders})",
            engine
        )

        recs = []
        for _, row in df_collab.iterrows():
            m_id = row['recommended_movie_id']
            m_data = df_movies[df_movies['id'] == m_id]
            m_title = m_data['title'].iloc[0] if not m_data.empty else "Unknown"
            poster_val = m_data['poster_url'].iloc[0] if not m_data.empty else None
            year_val = m_data['year'].iloc[0] if not m_data.empty else None

            boost = 1.0
            if m_id in df['id'].values:
                boost = get_kazakhstan_boost_score(df[df['id'] == m_id].iloc[0])

            recs.append({
                "movie_id": int(m_id),
                "score": float(row['watch_count']) * boost,
                "title": m_title,
                "poster_url": str(poster_val) if poster_val is not None and pd.notna(poster_val) else None,
                "year": _safe_year(year_val),
                "reason": "Люди также смотрели"
            })
        return recs
    except Exception as e:
        print(f"Collab error: {e}")
        return []

def get_domestic_recommendations(df, top_n=5):
    try:
        domestic = df[df['is_domestic'] == True].copy()
        if domestic.empty: return []
        domestic['rating_num'] = pd.to_numeric(domestic['imdb_rating'], errors='coerce').fillna(0)
        best = domestic.sort_values(by='rating_num', ascending=False).head(top_n)
        result = []
        for _, r in best.iterrows():
            poster = r['poster_url'] if 'poster_url' in r.index and pd.notna(r['poster_url']) else None
            year = _safe_year(r['year']) if 'year' in r.index else None
            result.append({"movie_id": int(r['id']), "score": float(r['rating_num']), "title": r['title'], "poster_url": poster, "year": year, "reason": "Казахстанское кино"})
        return result
    except Exception as e:
        print(f"get_domestic_recommendations error: {e}")
        return []

def _get_click_genre_weights(user_id, df, engine):
    """Genre weights from clicked movies — implicit interest signal."""
    try:
        clicks_df = pd.read_sql(
            text("SELECT DISTINCT movie_id FROM movie_clicks WHERE user_id = :user_id"),
            engine, params={"user_id": user_id}
        )
        if clicks_df.empty:
            return {}
        genre_counts = {}
        for m_id in clicks_df['movie_id']:
            row = df[df['id'] == m_id]
            if row.empty:
                continue
            for g in str(row['genre_clean'].iloc[0]).split(','):
                g = g.strip().lower()
                if g:
                    genre_counts[g] = genre_counts.get(g, 0) + 1
        return genre_counts
    except Exception:
        return {}


def _get_subtitle_kz_events(user_id, engine):
    """Count of KZ subtitle enable events — signal for Kazakh content affinity."""
    try:
        result = pd.read_sql(
            text("SELECT COUNT(*) AS cnt FROM subtitle_events WHERE user_id = :uid AND lang = 'kk' AND action = 'enable'"),
            engine, params={"uid": user_id}
        )
        return int(result['cnt'].iloc[0]) if not result.empty else 0
    except Exception:
        return 0


def _get_search_genre_weights(user_id, df, engine):
    """Genre weights from search queries — content preference signal."""
    try:
        searches_df = pd.read_sql(
            text("SELECT query FROM search_logs WHERE user_id = :user_id"),
            engine, params={"user_id": user_id}
        )
        if searches_df.empty:
            return {}
        known_genres = set()
        for genre_str in df['genre_clean'].fillna(''):
            for g in genre_str.split(','):
                g = g.strip().lower()
                if g:
                    known_genres.add(g)
        genre_counts = {}
        for query in searches_df['query']:
            query_lower = query.lower()
            for genre in known_genres:
                if genre in query_lower:
                    genre_counts[genre] = genre_counts.get(genre, 0) + 1
        return genre_counts
    except Exception:
        return {}


def _get_user_kz_affinity_boost(watched_movie_ids, df) -> float:
    """Return a personalized KZ boost multiplier based on what % of user's history is KZ content."""
    if not watched_movie_ids:
        return 1.3  # new user: moderate KZ introduction

    kz_count = 0
    total_count = 0
    for m_id in watched_movie_ids:
        row = df[df['id'] == m_id]
        if row.empty:
            continue
        total_count += 1
        country = str(row['country'].iloc[0]).lower() if 'country' in row.columns and pd.notna(row['country'].iloc[0]) else ''
        language = str(row['language'].iloc[0]).lower() if 'language' in row.columns and pd.notna(row['language'].iloc[0]) else ''
        if 'казахстан' in country or 'kazakhstan' in country or 'казахский' in language or 'kazakh' in language:
            kz_count += 1

    if total_count == 0:
        return 1.3

    kz_ratio = kz_count / total_count
    if kz_ratio >= 0.5:
        return 1.8   # KZ fan: maximize KZ content
    elif kz_ratio >= 0.2:
        return 1.4   # mixed taste: moderate KZ boost
    else:
        return 1.15  # mostly foreign: KZ still present, just not dominant


def _apply_personalized_kz_boost(movie_row, kz_affinity_boost: float) -> float:
    """Like get_kazakhstan_boost_score but with personalized KZ multiplier and Russia excluded."""
    country = str(movie_row.get('country', '')).lower() if pd.notna(movie_row.get('country', '')) else ''
    language = str(movie_row.get('language', '')).lower() if pd.notna(movie_row.get('language', '')) else ''

    if 'россия' in country or 'russia' in country:
        return DEFAULT_BOOST  # Russia: no boost

    if 'казахстан' in country or 'kazakhstan' in country or 'kz' in country:
        return kz_affinity_boost
    if 'казахский' in language or 'kazakh' in language or 'қазақ' in language:
        return kz_affinity_boost
    if any(c in country for c in ['кыргыз', 'узбек', 'таджик', 'туркмен']):
        return CENTRAL_ASIA_BOOST
    if any(c in country for c in ['беларусь', 'украина', 'азербайджан']):
        return CIS_BOOST
    return DEFAULT_BOOST


def get_collaborative_recommendations(user_id, df, cosine_sim, top_n=5):
    import numpy as np

    query = text("SELECT movie_id, seconds_watched, completed FROM watch_history WHERE user_id = :user_id")
    try:
        history_df = pd.read_sql(query, engine, params={"user_id": user_id})
    except:
        return None

    if history_df.empty:
        return None

    user_profile_scores = np.zeros(len(df))
    watched_movie_ids = history_df['movie_id'].tolist()

    for _, row in history_df.iterrows():
        m_id = row['movie_id']
        seconds = row['seconds_watched'] if pd.notna(row['seconds_watched']) else 0
        completed = row['completed']

        weight = 0.5
        if completed: weight = 2.0
        elif seconds > 0: weight = 0.5 + min((seconds / 7200.0), 1.0)

        try:
            idx = df.index[df['id'] == m_id][0]
            user_profile_scores += weight * cosine_sim[idx]
        except IndexError:
            continue

    # Enrich profile with click genre signals
    click_genres = _get_click_genre_weights(user_id, df, engine)

    merged_genres = {}
    for g, w in click_genres.items():
        merged_genres[g] = merged_genres.get(g, 0) + w * 0.3

    if merged_genres:
        max_w = max(merged_genres.values())
        if max_w > 0:
            for i in range(len(df)):
                movie_genres = set(g.strip().lower() for g in df['genre_clean'].iloc[i].split(',') if g.strip())
                genre_boost = sum(merged_genres.get(g, 0) for g in movie_genres)
                user_profile_scores[i] += (genre_boost / max_w) * 0.2

    for m_id in watched_movie_ids:
        try:
            idx = df.index[df['id'] == m_id][0]
            user_profile_scores[idx] = 0.0
        except IndexError:
            pass

    personal_kz_boost = _get_user_kz_affinity_boost(watched_movie_ids, df)
    for i in range(len(df)):
        user_profile_scores[i] *= _apply_personalized_kz_boost(df.iloc[i], personal_kz_boost)

    # Subtitle KZ signal: each kk-subtitle enable adds a small extra boost to KZ films
    kz_subtitle_count = _get_subtitle_kz_events(user_id, engine)
    if kz_subtitle_count > 0:
        subtitle_boost = min(kz_subtitle_count * 0.05, 0.3)  # cap at +30%
        for i in range(len(df)):
            row = df.iloc[i]
            country = str(row.get('country', '')).lower()
            language = str(row.get('language', '')).lower()
            if 'казахстан' in country or 'kazakhstan' in country or 'казахский' in language or 'kazakh' in language:
                user_profile_scores[i] *= (1.0 + subtitle_boost)

    if user_profile_scores.max() == 0:
        return None
    candidates = user_profile_scores.argsort()[::-1][:top_n * 3]
    cand_reasons = ["" for _ in candidates]
    indices, scores, reasons = _apply_kz_ratio(df, candidates, user_profile_scores[candidates], cand_reasons, top_n)
    return _format_recs(df, indices, scores, reasons)

def get_youtube_like_feed(user_id, df, cosine_sim):
    # Old logic kept intact
    import numpy as np

    query = text("SELECT movie_id, seconds_watched, completed, id as watch_id FROM watch_history WHERE user_id = :user_id ORDER BY id DESC")
    try:
        history_df = pd.read_sql(query, engine, params={"user_id": user_id})
    except:
        return {"top_picks_for_you": get_popular_fallback(df, top_n=5)}
    
    feed = {
        "continue_watching": [],
        "up_next": None,
        "because_you_watched": None,
        "top_picks_for_you": [],
        "trending": get_popular_fallback(df, top_n=10)
    }
    
    if history_df.empty:
        feed["top_picks_for_you"] = feed["trending"][:5]
        return feed
        
    watched_movie_ids = history_df['movie_id'].tolist()
    
    in_progress = history_df[~history_df['completed'].fillna(False).astype(bool)]
    for _, row in in_progress.head(5).iterrows():
        try:
            m_idx = df.index[df['id'] == row['movie_id']][0]
            feed["continue_watching"].append({
                "movie_id": int(row['movie_id']),
                "title": df['title'].iloc[m_idx],
                "seconds_watched": int(row['seconds_watched']) if pd.notna(row['seconds_watched']) else 0
            })
        except IndexError: pass

    latest_watched = history_df.head(1)
    if not latest_watched.empty:
        last_row = latest_watched.iloc[0]
        is_essentially_completed = last_row['completed'] or (pd.notna(last_row['seconds_watched']) and last_row['seconds_watched'] > 720)
        
        if is_essentially_completed:
            last_completed_id = last_row['movie_id']
            try:
                last_idx = df.index[df['id'] == last_completed_id][0]
                last_title = df['title_clean'].iloc[last_idx]
                
                sim_scores = cosine_sim[last_idx].copy()
                for w_id in watched_movie_ids:
                    try:
                        w_idx = df.index[df['id'] == w_id][0]
                        sim_scores[w_idx] = -1.0
                    except IndexError: pass
                    
                base_prefix = last_title.split(":")[0] if ":" in last_title else " ".join(last_title.split(" ")[:2])
                
                for i in range(len(df)):
                    title = df['title_clean'].iloc[i]
                    if title != last_title and title.startswith(base_prefix):
                        sim_scores[i] += 0.5
                        
                top_next_idx = sim_scores.argmax()
                if sim_scores[top_next_idx] > 0:
                    feed["up_next"] = {
                        "reason": f"Продолжить просмотр франшизы / логический следующий шаг после «{last_title}»",
                        "recommendation": {
                            "movie_id": int(df['id'].iloc[top_next_idx]),
                            "title": df['title'].iloc[top_next_idx],
                            "score": round(float(sim_scores[top_next_idx]), 3)
                        }
                    }
            except IndexError: pass

    random_history_movie = history_df.sample(n=1).iloc[0]['movie_id']
    try:
        r_idx = df.index[df['id'] == random_history_movie][0]
        r_title = df['title'].iloc[r_idx]
        
        raw_recs = get_content_recommendations(random_history_movie, df, cosine_sim, top_n=15)
        if raw_recs:
            filtered_recs = [r for r in raw_recs if r['movie_id'] not in watched_movie_ids][:5]
            if filtered_recs:
                feed["because_you_watched"] = {
                    "reason": f"Похожее на то, что вы смотрели: «{r_title}»",
                    "recommendations": filtered_recs
                }
    except Exception: pass
        
    top_picks = get_collaborative_recommendations(user_id, df, cosine_sim, top_n=10)
    if top_picks:
        used_ids = set()
        if feed["up_next"]: used_ids.add(feed["up_next"]["recommendation"]["movie_id"])
            
        clean_top_picks = []
        for pick in top_picks:
            if pick["movie_id"] not in used_ids:
                clean_top_picks.append(pick)
                used_ids.add(pick["movie_id"])
                
        feed["top_picks_for_you"] = clean_top_picks[:5]
        
    return feed


# ==============================================================
# НОВЫЕ УЛУЧШЕННЫЕ ФУНКЦИИ
# ==============================================================

def get_smart_hybrid_recommendations(movie_id, df, cosine_sim, top_n=5, user_id=None):
    """Multi-stage pipeline: candidate generation -> scoring -> re-ranking"""
    if movie_id not in df["id"].values:
        return []

    target_idx = df.index[df["id"] == movie_id][0]
    target_director = df["director_clean"].iloc[target_idx].strip().lower()
    target_actors = set(a.strip().lower() for a in df["actors_clean"].iloc[target_idx].split(",") if a.strip())
    target_genres = set(g.strip().lower() for g in df["genre_clean"].iloc[target_idx].split(",") if g.strip())

    sim_row = cosine_sim[target_idx].copy()
    sim_row[target_idx] = -1.0
    candidate_indices = sim_row.argsort()[::-1][:200]

    scores = []
    for i in candidate_indices:
        if i == target_idx:
            continue
        movie_director = df["director_clean"].iloc[i].strip().lower()
        movie_actors = set(a.strip().lower() for a in df["actors_clean"].iloc[i].split(",") if a.strip())
        movie_genres = set(g.strip().lower() for g in df["genre_clean"].iloc[i].split(",") if g.strip())

        shared_genres = len(target_genres.intersection(movie_genres))
        genre_score = shared_genres / max(1, len(target_genres)) if target_genres else 0.0
        content_score = float(cosine_sim[target_idx][i])
        director_score = 1.0 if (target_director and target_director == movie_director) else 0.0
        shared_actors = len(target_actors.intersection(movie_actors))
        actor_score = min(1.0, shared_actors * 0.25)
        rating_boost = float(df["rating_norm"].iloc[i]) if "rating_norm" in df.columns else 0.0

        base_score = (genre_score * 0.30 + content_score * 0.25 +
                 director_score * 0.20 + actor_score * 0.15 + rating_boost * 0.10)
        boost = get_kazakhstan_boost_score(df.iloc[i])
        score = base_score * boost
        scores.append((i, score))

    scores.sort(key=lambda x: x[1], reverse=True)

    watched_ids = set()
    if user_id is not None:
        try:
        
            wh = pd.read_sql(text("SELECT movie_id FROM watch_history WHERE user_id = :user_id"), engine, params={"user_id": user_id})
            watched_ids = set(wh["movie_id"].tolist())
        except Exception:
            pass

    result_indices, result_scores, reasons = [], [], []
    seen_titles = set()
    for (i, s) in scores:
        m_id = int(df["id"].iloc[i])
        title_key = df["title_clean"].iloc[i].lower()[:30]
        if m_id in watched_ids or title_key in seen_titles:
            continue
        seen_titles.add(title_key)

        parts = []
        shared = [g.strip() for g in df["genre_clean"].iloc[i].split(",") if g.strip().lower() in target_genres]
        if shared:
            parts.append("жанры: " + ", ".join(shared[:2]))
        if df["director_clean"].iloc[i].strip().lower() == target_director and target_director:
            parts.append(f"режиссёр: {df['director_clean'].iloc[i].strip()}")
        if df["is_domestic"].iloc[i]:
            parts.append("казахское кино")
        reasons.append("Умная рекомендация: " + ", ".join(parts) if parts else "Умная рекомендация")
        result_indices.append(i)
        result_scores.append(s)
        if len(result_indices) >= top_n:
            break

    return _format_recs(df, result_indices, result_scores, reasons)


def get_because_you_liked(user_id, df, cosine_sim, top_n=5):
    """Рекомендации на основе высоко оценённых фильмов (>= 7)"""

    try:
        ratings_df = pd.read_sql(
            text("SELECT movie_id, score FROM ratings WHERE user_id = :user_id AND score >= 7 ORDER BY score DESC LIMIT 10"),
            engine, params={"user_id": user_id}
        )
    except Exception as e:
        print(f"because_you_liked error: {e}")
        return []

    if ratings_df.empty:
        return []

    try:
        wh = pd.read_sql(text("SELECT movie_id FROM watch_history WHERE user_id = :user_id"), engine, params={"user_id": user_id})
        watched_ids = set(wh["movie_id"].tolist())
    except Exception:
        watched_ids = set()

    profile = np.zeros(len(df))
    best_movie_id, best_score_val = None, 0
    for _, row in ratings_df.iterrows():
        m_id = row["movie_id"]
        if m_id not in df["id"].values:
            continue
        idx = df.index[df["id"] == m_id][0]
        profile += (float(row["score"]) / 10.0) * cosine_sim[idx]
        if row["score"] > best_score_val:
            best_score_val = row["score"]
            best_movie_id = m_id

    for m_id in watched_ids:
        if m_id in df["id"].values:
            profile[df.index[df["id"] == m_id][0]] = 0.0

    for i in range(len(df)):
        profile[i] *= get_kazakhstan_boost_score(df.iloc[i])

    if profile.max() == 0:
        return []

    candidates = profile.argsort()[::-1][:top_n * 3]
    best_title = df[df["id"] == best_movie_id]["title"].iloc[0] if best_movie_id else "фильма"
    cand_reasons = [f"Потому что вам понравился «{best_title}»" for _ in candidates]
    indices, scores, reasons = _apply_kz_ratio(df, candidates, profile[candidates], cand_reasons, top_n)
    return _format_recs(df, indices, scores, reasons)


def get_kazakhstan_tab_recommendations(df, user_id=None, limit=20, genre=None):

    watched_ids = set()
    if user_id is not None:
        try:
            wh = pd.read_sql(text("SELECT movie_id FROM watch_history WHERE user_id = :user_id"), engine, params={"user_id": user_id})
            watched_ids = set(wh["movie_id"].tolist())
        except Exception:
            pass

    kz_df = df.copy()
    scores = []
    reasons = []
    indices = []
    
    for i in range(len(kz_df)):
        m_id = int(kz_df['id'].iloc[i])
        if m_id in watched_ids:
            continue
        
        country = str(kz_df.get("country", "").iloc[i]).lower() if 'country' in kz_df.columns and pd.notna(kz_df['country'].iloc[i]) else ""
        language = str(kz_df.get("language", "").iloc[i]).lower() if 'language' in kz_df.columns and pd.notna(kz_df['language'].iloc[i]) else ""
        
        is_kz = False
        if "казахстан" in country or "kazakhstan" in country or "kz" in country:
            is_kz = True
        if "казахский" in language or "kazakh" in language or "қазақ" in language:
            is_kz = True
            
        if is_kz:
            if genre and genre.lower() not in str(kz_df['genre_clean'].iloc[i]).lower():
                continue
            boost = get_kazakhstan_boost_score(kz_df.iloc[i])
            rating_num = float(kz_df['rating_norm'].iloc[i]) if 'rating_norm' in kz_df.columns else 0.0
            scores.append(rating_num * boost)
            reasons.append("Казахское кино")
            indices.append(i)
            
    if not indices:
        return []
        
    import numpy as np
    indices = np.array(indices)
    scores = np.array(scores)
    top_idxs = indices[scores.argsort()[::-1][:limit]]
    top_scores = scores[scores.argsort()[::-1][:limit]]
    top_reasons = [reasons[0]] * len(top_idxs)
    
    return _format_recs(kz_df, top_idxs, top_scores, top_reasons)
