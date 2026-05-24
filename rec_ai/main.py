from fastapi import FastAPI, Depends, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from typing import List, Dict, Any, Optional
from contextlib import asynccontextmanager
import os

from database import SessionLocal
from models import Movie, User, Rating, WatchHistory
import recommender
import schemas

ML_MODEL = {
    "df": None,
    "similarity": None
}

def load_models():
    try:
        df, sim = recommender.get_recommendations_model()
        ML_MODEL["df"] = df
        ML_MODEL["similarity"] = sim
        print("Model loaded successfully into memory.")
    except Exception as e:
        print(f"Error loading models: {e}")

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    load_models()
    yield
    # Shutdown (nothing to do)

app = FastAPI(title="Cinema AI Service", version="3.0.0", lifespan=lifespan)

origins = os.getenv("ALLOWED_ORIGINS", "http://localhost:5173,http://localhost:3000,http://localhost:5174").split(",")

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


@app.post("/api/v1/ml/retrain")
async def retrain_models(background_tasks: BackgroundTasks):
    background_tasks.add_task(load_models)
    return {"status": "retrain_started", "message": "Модель переобучается в фоне"}

@app.get("/health", response_model=schemas.HealthResponse)
async def health_check():
    kz_count = 0
    if ML_MODEL["df"] is not None:
        try:
            df = ML_MODEL["df"]
            country_mask = df['country'].fillna('').str.lower().str.contains('казахстан|kazakhstan', na=False)
            lang_mask = df['language'].fillna('').str.lower().str.contains('казахский|kazakh', na=False)
            kz_count = int((country_mask | lang_mask).sum())
        except Exception:
            pass
            
    return {
        "status": "ok",
        "model_loaded": ML_MODEL["df"] is not None,
        "movies_in_memory": len(ML_MODEL["df"]) if ML_MODEL["df"] is not None else 0,
        "kazakhstan_movies": kz_count,
        "version": "1.2.0",
        "db_connected": True
    }

@app.get("/api/v1/stats")
async def get_ml_stats(db: Session = Depends(get_db)):
    kz_count = 0
    if ML_MODEL["df"] is not None:
        try:
            df = ML_MODEL["df"]
            country_mask = df['country'].fillna('').str.lower().str.contains('казахстан|kazakhstan', na=False)
            lang_mask = df['language'].fillna('').str.lower().str.contains('казахский|kazakh', na=False)
            kz_count = int((country_mask | lang_mask).sum())
        except Exception:
            pass
            
    return {
        "total_movies": db.query(Movie).count(),
        "kazakhstan_movies": kz_count,
        "model_version": "1.2.0",
        "last_retrain": "2024-01-15T10:00:00Z",
        "recommendation_tabs": ["popular", "new", "kazakhstan", "because-you-liked", "smart"]
    }

def check_model():
    if ML_MODEL["df"] is None:
        raise HTTPException(status_code=503, detail="Модель обучается")

# ====================
# METRICS
# ====================
@app.post("/api/v1/metrics/click")
async def log_click(event: schemas.ClickEvent):
    print(f"Metrics Click: user_id={event.user_id}, movie_id={event.movie_id}, source={event.source}")
    return {"status": "ok"}

@app.post("/api/v1/metrics/search")
async def log_search(event: schemas.SearchEvent):
    print(f"Metrics Search: user_id={event.user_id}, query={event.query}")
    return {"status": "ok"}

@app.post("/api/v1/metrics/subtitle")
async def log_subtitle(event: schemas.SubtitleEvent):
    print(f"Metrics Subtitle: user_id={event.user_id}, movie_id={event.movie_id}, action={event.action}")
    return {"status": "ok"}

# ====================
# TABS RECOMMENDATIONS
# ====================

@app.get("/api/v1/recommend/tab/franchise/{movie_id}", deprecated=True)
async def recommend_franchise(movie_id: int, limit: int = 5):
    check_model()
    recs = recommender.get_franchise_recommendations(movie_id, ML_MODEL["df"], limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "franchise", "deprecated": True, "use_instead": "/api/v1/recommendations/tab/franchise"}

@app.get("/api/v1/recommend/tab/director/{movie_id}", deprecated=True)
async def recommend_director(movie_id: int, limit: int = 5):
    check_model()
    recs = recommender.get_director_recommendations(movie_id, ML_MODEL["df"], limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "director"}

@app.get("/api/v1/recommend/tab/actor/{movie_id}", deprecated=True)
async def recommend_actor(movie_id: int, limit: int = 5):
    check_model()
    recs = recommender.get_actor_recommendations(movie_id, ML_MODEL["df"], limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "actor_match"}

@app.get("/api/v1/recommend/tab/genre/{movie_id}", deprecated=True)
async def recommend_genre(movie_id: int, limit: int = 5):
    check_model()
    recs = recommender.get_genre_recommendations(movie_id, ML_MODEL["df"], limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "genre_match"}

@app.get("/api/v1/recommend/tab/content/{movie_id}", deprecated=True)
async def recommend_content(movie_id: int, limit: int = 5):
    check_model()
    recs = recommender.get_content_recommendations(movie_id, ML_MODEL["df"], ML_MODEL["similarity"], limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "content_based_similarity"}

@app.get("/api/v1/recommend/tab/hybrid/{movie_id}", deprecated=True)
async def recommend_hybrid(movie_id: int, limit: int = 5):
    check_model()
    recs = recommender.get_hybrid_recommendations(movie_id, ML_MODEL["df"], ML_MODEL["similarity"], limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "hybrid_scoring"}

@app.get("/api/v1/recommend/tab/smart/{movie_id}", deprecated=True)
async def recommend_smart(movie_id: int, limit: int = 5, user_id: Optional[int] = None):
    check_model()
    recs = recommender.get_smart_hybrid_recommendations(
        movie_id, ML_MODEL["df"], ML_MODEL["similarity"], limit, user_id
    )
    if not recs:
        recs = recommender.get_hybrid_recommendations(movie_id, ML_MODEL["df"], ML_MODEL["similarity"], limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "smart_hybrid"}

@app.get("/api/v1/recommend/tab/collaborative-item/{movie_id}", deprecated=True)
async def recommend_collaborative_item(movie_id: int, limit: int = 5):
    recs = recommender.get_collaborative_users_also_watched(movie_id, limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "collaborative_item_based"}

@app.get("/api/v1/recommend/tab/domestic", deprecated=True)
async def recommend_domestic(limit: int = 5):
    check_model()
    recs = recommender.get_domestic_recommendations(ML_MODEL["df"], limit)
    return {"recommendations": recs, "method": "domestic_cinema"}

@app.get("/api/v1/recommend/tab/because-you-liked/{user_id}", deprecated=True)
async def recommend_because_you_liked(user_id: int, limit: int = 5):
    check_model()
    recs = recommender.get_because_you_liked(user_id, ML_MODEL["df"], ML_MODEL["similarity"], limit)
    if not recs:
        recs = recommender.get_collaborative_recommendations(user_id, ML_MODEL["df"], ML_MODEL["similarity"], limit)
    if not recs:
        recs = recommender.get_popular_fallback(ML_MODEL["df"], limit)
    return {"user_id": user_id, "recommendations": recs, "method": "because_you_liked"}

# ====================
# OLD RECOMMENDATIONS (COMPATIBILITY)
# ====================

@app.get("/api/v1/recommend/collaborative/{user_id}", deprecated=True)
async def recommend_collaborative_user(user_id: int, limit: int = 5):
    check_model()
    recs = recommender.get_collaborative_recommendations(user_id, ML_MODEL["df"], ML_MODEL["similarity"], limit)
    if not recs:
        recs = recommender.get_popular_fallback(ML_MODEL["df"], limit)
    return {"user_id": user_id, "recommendations": recs}

@app.get("/api/v1/recommend/feed/{user_id}", deprecated=True)
async def get_smart_feed(user_id: int):
    check_model()
    try:
        feed = recommender.get_youtube_like_feed(user_id, ML_MODEL["df"], ML_MODEL["similarity"])
    except Exception as e:
        print(f"Feed error for user {user_id}: {e}")
        feed = {"top_picks_for_you": recommender.get_popular_fallback(ML_MODEL["df"], top_n=5)}
    return {"user_id": user_id, "feed": feed}

@app.get("/api/v1/movie/details/{movie_id}")
async def get_movie_details(movie_id: int, db: Session = Depends(get_db)):
    movie = db.query(Movie).filter(Movie.id == movie_id).first()
    if not movie:
        raise HTTPException(status_code=404, detail="Фильм не найден")
    return {
        "id": movie.id,
        "title": movie.title,
        "description": movie.description,
        "genres": movie.genre_text,
        "director": movie.director,
        "actors": movie.actors,
        "is_domestic": movie.is_domestic,
        "imdb_rating": movie.imdb_rating
    }


# ==============================================================
# ЭНДПОИНТЫ ДЛЯ ПРЯМОГО ПОДКЛЮЧЕНИЯ ФРОНТЕНДА (без Java прокси)
# ==============================================================

@app.get("/api/v1/recommendations/feed")
async def frontend_smart_feed(user_id: Optional[int] = None):
    check_model()
    if user_id is None:
        return {"feed": {"top_picks_for_you": recommender.get_popular_fallback(ML_MODEL["df"], top_n=10)}}
    try:
        feed = recommender.get_youtube_like_feed(user_id, ML_MODEL["df"], ML_MODEL["similarity"])
    except Exception as e:
        print(f"Feed error for user {user_id}: {e}")
        feed = {"top_picks_for_you": recommender.get_popular_fallback(ML_MODEL["df"], top_n=10)}
    return {"user_id": user_id, "feed": feed}

@app.get("/api/v1/recommendations/tab/kazakhstan", response_model=schemas.RecommendationResponse)
async def frontend_kazakhstan_tab(user_id: Optional[int] = None, limit: int = 20, genre: Optional[str] = None):
    check_model()
    recs = recommender.get_kazakhstan_tab_recommendations(ML_MODEL["df"], user_id=user_id, limit=limit, genre=genre)
    return {"recommendations": recs, "method": "kazakhstan", "total": len(recs)}


@app.get("/api/v1/recommendations/tab/{type}")
async def frontend_tab_recommendations_no_movie_id(type: str, limit: int = 15, user_id: Optional[int] = None, tab: Optional[str] = None, genre: Optional[str] = None):
    check_model()
    request_type = tab if tab else type
    
    if request_type == "new" or request_type == "popular":
        recs = recommender.get_popular_fallback(ML_MODEL["df"], limit)
        return {"recommendations": recs, "method": request_type}
    elif request_type == "comedies" or request_type == "comedy":
        recs = recommender.get_popular_fallback(ML_MODEL["df"], limit)
        return {"recommendations": recs, "method": request_type}
        
    raise HTTPException(status_code=400, detail=f"Неизвестный тип: {request_type}")

@app.get("/api/v1/recommendations/tab")
async def frontend_tab_recommendations_query(tab: str, limit: int = 15, user_id: Optional[int] = None, genre: Optional[str] = None):
    check_model()
    if tab == "new" or tab == "popular":
        recs = recommender.get_popular_fallback(ML_MODEL["df"], limit)
        return {"recommendations": recs, "method": tab}
    elif tab == "comedies" or tab == "comedy":
        recs = recommender.get_popular_fallback(ML_MODEL["df"], limit)
        return {"recommendations": recs, "method": tab}
    elif tab == "kazakhstan":
        recs = recommender.get_kazakhstan_tab_recommendations(ML_MODEL["df"], user_id=user_id, limit=limit, genre=genre)
        return {"recommendations": recs, "method": tab}
        
    raise HTTPException(status_code=400, detail=f"Неизвестный тип: {tab}")


@app.get("/api/v1/recommendations/tab/{type}/{movie_id}", response_model=schemas.RecommendationResponse)
async def frontend_tab_recommendations(type: str, movie_id: int, limit: int = 15, user_id: Optional[int] = None):
    check_model()
    try:
        result = {
            "franchise":          lambda: recommender.get_franchise_recommendations(movie_id, ML_MODEL["df"], limit),
            "director":           lambda: recommender.get_director_recommendations(movie_id, ML_MODEL["df"], limit),
            "actor":              lambda: recommender.get_actor_recommendations(movie_id, ML_MODEL["df"], limit),
            "genre":              lambda: recommender.get_genre_recommendations(movie_id, ML_MODEL["df"], limit),
            "content":            lambda: recommender.get_content_recommendations(movie_id, ML_MODEL["df"], ML_MODEL["similarity"], limit),
            "hybrid":             lambda: recommender.get_hybrid_recommendations(movie_id, ML_MODEL["df"], ML_MODEL["similarity"], limit),
            "smart":              lambda: recommender.get_smart_hybrid_recommendations(movie_id, ML_MODEL["df"], ML_MODEL["similarity"], limit, user_id),
            "collaborative-item": lambda: recommender.get_collaborative_users_also_watched(movie_id, limit),
        }.get(type)
        if result is None:
            raise HTTPException(status_code=400, detail=f"Неизвестный тип: {type}")
        recs = result()
        if not recs and type == "smart":
            recs = recommender.get_hybrid_recommendations(movie_id, ML_MODEL["df"], ML_MODEL["similarity"], limit)
        return {"movie_id": movie_id, "recommendations": recs, "method": type, "total": len(recs)}
    except HTTPException:
        raise
    except Exception as e:
        print(f"Tab rec error [{type}] movie={movie_id}: {e}")
        return {"movie_id": movie_id, "recommendations": [], "method": type, "total": 0}

@app.get("/api/v1/recommendations/tab/because-you-liked", response_model=schemas.RecommendationResponse)
async def frontend_because_you_liked(user_id: int, limit: int = 15):
    check_model()
    recs = recommender.get_because_you_liked(user_id, ML_MODEL["df"], ML_MODEL["similarity"], limit)
    if not recs:
        recs = recommender.get_collaborative_recommendations(user_id, ML_MODEL["df"], ML_MODEL["similarity"], limit)
    if not recs:
        recs = recommender.get_popular_fallback(ML_MODEL["df"], limit)
    return {"user_id": user_id, "recommendations": recs, "method": "because_you_liked", "total": len(recs)}

@app.get("/api/v1/trending", response_model=schemas.RecommendationResponse)
async def get_trending(weekly: bool = False):
    check_model()
    recs = recommender.get_popular_fallback(ML_MODEL["df"], 20)
    return {"recommendations": recs, "method": "trending", "total": len(recs)}

@app.get("/api/v1/trending/weekly", response_model=schemas.RecommendationResponse)
async def get_trending_weekly():
    check_model()
    recs = recommender.get_popular_fallback(ML_MODEL["df"], 20)
    return {"recommendations": recs, "method": "trending_weekly", "total": len(recs)}

@app.get("/api/v1/recommendations/movie/{movie_id}")
async def frontend_right_rail(movie_id: int, limit: int = 15, user_id: Optional[int] = None):
    check_model()
    recs = recommender.get_smart_hybrid_recommendations(movie_id, ML_MODEL["df"], ML_MODEL["similarity"], limit, user_id)
    if not recs:
        recs = recommender.get_hybrid_recommendations(movie_id, ML_MODEL["df"], ML_MODEL["similarity"], limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "smart_hybrid"}
