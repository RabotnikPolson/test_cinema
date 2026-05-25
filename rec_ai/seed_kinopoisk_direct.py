import os
import urllib.request
import json
import time
from sqlalchemy import create_engine, text

DB_USER = os.getenv("DB_USER", "test_user")
DB_PASS = os.getenv("DB_PASS", "pass1")
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "testdb")
DATABASE_URL = f"postgresql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

# Read API Key from application.properties
API_KEY = os.getenv("KINOPOISK_API_KEY")
if not API_KEY:
    raise ValueError("KINOPOISK_API_KEY not set in environment")

HEADERS = {
    "X-API-KEY": API_KEY,
    "Content-Type": "application/json",
}

def fetch_film(kp_id):
    url = f"https://kinopoiskapiunofficial.tech/api/v2.2/films/{kp_id}"
    req = urllib.request.Request(url, headers=HEADERS)
    try:
        with urllib.request.urlopen(req) as response:
            if response.status == 200:
                return json.loads(response.read().decode())
    except Exception:
        pass
    return None

def fetch_staff(kp_id):
    url = f"https://kinopoiskapiunofficial.tech/api/v1/staff?filmId={kp_id}"
    req = urllib.request.Request(url, headers=HEADERS)
    try:
        with urllib.request.urlopen(req) as response:
            if response.status == 200:
                return json.loads(response.read().decode())
    except Exception:
        pass
    return []

# 150 Kinopoisk IDs
POPULAR_FRANCHISES = [
    324, 689, 326, 8131, 277328, 397667, 405609, 415356, # Harry Potter
    688, 312, 3498, # LotR
    599, 584, # Taxi
    258687, # Interstellar
    435, 436, # The Green Mile, Shawshank Redemption
    448, # Forrest Gump
    325, # Matrix
    251733, # Avatar
    2360, # Lion King
    505898, # Avatar 2
    81338, # Django
    462682, # Wolf of Wall Street
]

KAZAKH_MOVIES = [
    257906, # Рэкетир
    819446, # Рэкетир 2
    906806, # Бизнес по-казахски
    1048472, # Бизнес по-казахски в Америке
    1222444, # Бизнес по-казахски в Корее
    675451, # Шал
    738734, # Келинка Сабина
    1045233, # Келинка Сабина 2
    4873, # Игла (СССР/Казахстан)
]

# Random other popular kinopoisk IDs to make it up to ~50
EXTRA_IDS = [
    111543, 666, 258809, 361, 843650, 444, 4374, 535341, 679486, 1143242, 608502,
    775276, 571896, 601, 88222, 606, 5060, 26033, 4976, 256, 312, 1045186, 76273,
    8100, 462762, 271502, 361, 255557, 1009536, 1267348, 461, 645118, 595901
]

ALL_IDS = list(set(POPULAR_FRANCHISES + KAZAKH_MOVIES + EXTRA_IDS))

def capitalize(s):
    if not s: return ""
    return s[0].upper() + s[1:]

def seed_db():
    engine = create_engine(DATABASE_URL)
    with engine.begin() as conn:
        for kp_id in ALL_IDS:
            print(f"Fetching {kp_id}...")
            data = fetch_film(kp_id)
            if not data:
                print(f"-> Failed or not found.")
                continue

            # Extract fields
            title = data.get("nameRu") or data.get("nameEn") or data.get("nameOriginal") or "Без названия"
            description = data.get("description", "")
            poster_url = data.get("posterUrl", "")
            year = data.get("year", None)
            imdb_id = data.get("imdbId", None)
            
            rating_kp = data.get("ratingKinopoisk")
            rating_imdb = data.get("ratingImdb")
            imdb_rating = str(rating_kp) if rating_kp is not None else (str(rating_imdb) if rating_imdb is not None else None)
            
            votes = data.get("ratingKinopoiskVoteCount")
            imdb_votes = str(votes) if votes else None
            
            fl = data.get("filmLength")
            runtime = f"{fl} мин" if isinstance(fl, int) else str(fl) if fl else None
            
            slogan = data.get("slogan")
            type_lang = data.get("type")
            
            countries = data.get("countries", [])
            country = countries[0].get("country") if countries else None
            
            is_domestic = country in ["Казахстан", "СССР"]

            # Genres
            genres_list = data.get("genres", [])
            genre_names = [capitalize(g.get("genre", "").strip()) for g in genres_list if g.get("genre", "").strip()]
            genre_text = ", ".join(genre_names)

            # Staff
            staff = fetch_staff(kp_id)
            directors = []
            actors = []
            for person in staff:
                prof = person.get("professionKey", "")
                name = person.get("nameRu") or person.get("nameEn") or ""
                name = name.strip()
                if not name: continue
                
                if prof == "DIRECTOR":
                    directors.append(name)
                elif prof == "ACTOR" and len(actors) < 7:
                    actors.append(name)
                    
            dir_text = ", ".join(directors)
            act_text = ", ".join(actors)
            
            # Insert into database
            query = text("""
                INSERT INTO movies (kinopoisk_id, title, description, poster_url, year, imdb_id, 
                                    imdb_rating, imdb_votes, runtime, released, language, country, 
                                    genre_text, director, actors, is_domestic)
                VALUES (:kp, :t, :d, :poster, :y, :imdb, :rat, :v, :run, :slogan, :lang, :c, :gt, :dir, :act, :dom)
                RETURNING id
            """)
            
            res = conn.execute(query, {
                "kp": str(kp_id), "t": title, "d": description, "poster": poster_url, "y": year, 
                "imdb": imdb_id, "rat": imdb_rating, "v": imdb_votes, "run": runtime, "slogan": slogan, 
                "lang": type_lang, "c": country, "gt": genre_text, "dir": dir_text, "act": act_text, 
                "dom": is_domestic
            })
            movie_internal_id = res.scalar()
            
            # Sync genres
            for gname in genre_names:
                g_res = conn.execute(text("SELECT id FROM genres WHERE name = :n"), {"n": gname}).first()
                if g_res:
                    g_id = g_res[0]
                else:
                    g_id = conn.execute(text("INSERT INTO genres (name) VALUES (:n) RETURNING id"), {"n": gname}).scalar()
                
                # Link
                conn.execute(text("INSERT INTO movie_genres (movie_id, genre_id) VALUES (:m, :g) ON CONFLICT DO NOTHING"), 
                            {"m": movie_internal_id, "g": g_id})
                            
            print(f"-> Saved '{title}' (id={movie_internal_id})")
            time.sleep(0.3) # Avoid hammering the API

if __name__ == "__main__":
    seed_db()
    print("Retraining ML model...")
    try:
        req = urllib.request.Request("http://localhost:8000/api/v1/ml/retrain", method="POST")
        urllib.request.urlopen(req)
    except Exception:
        pass
    print("Done!")
