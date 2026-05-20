from fastapi import FastAPI, Depends, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from typing import List, Dict, Any

from database import SessionLocal
from models import Movie, User, Rating, WatchHistory
import recommender

app = FastAPI(title="Cinema AI Service", version="3.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

ML_MODEL = {
    "df": None,
    "similarity": None
}

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def load_models():
    try:
        df, sim = recommender.get_recommendations_model()
        ML_MODEL["df"] = df
        ML_MODEL["similarity"] = sim
        print("Model loaded successfully into memory.")
    except Exception as e:
        print(f"Error loading models: {e}")

@app.on_event("startup")
async def startup_event():
    load_models()

@app.post("/api/v1/ml/retrain")
async def retrain_models(background_tasks: BackgroundTasks):
    background_tasks.add_task(load_models)
    return {"_comment_action": "Процесс переобучения запущен в фоновом режиме", "status": "ok"}

@app.get("/api/v1/stats")
async def get_ml_stats(db: Session = Depends(get_db)):
    try:
        return {
            "movies": db.query(Movie).count(),
            "users": db.query(User).count(),
            "ratings": db.query(Rating).count(),
            "history": db.query(WatchHistory).count()
        }
    except Exception as e:
        # Tables may not exist yet (e.g. Java migrations haven't run)
        return {"movies": 0, "users": 0, "ratings": 0, "history": 0, "warning": "DB tables not ready"}

def check_model():
    if ML_MODEL["df"] is None:
        raise HTTPException(status_code=503, detail="Модель обучается")

# ====================
# TABS RECOMMENDATIONS
# ====================

@app.get("/api/v1/recommend/tab/franchise/{movie_id}")
async def recommend_franchise(movie_id: int, limit: int = 5):
    check_model()
    recs = recommender.get_franchise_recommendations(movie_id, ML_MODEL["df"], limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "franchise"}

@app.get("/api/v1/recommend/tab/director/{movie_id}")
async def recommend_director(movie_id: int, limit: int = 5):
    check_model()
    recs = recommender.get_director_recommendations(movie_id, ML_MODEL["df"], limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "director"}

@app.get("/api/v1/recommend/tab/actor/{movie_id}")
async def recommend_actor(movie_id: int, limit: int = 5):
    check_model()
    recs = recommender.get_actor_recommendations(movie_id, ML_MODEL["df"], limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "actor_match"}

@app.get("/api/v1/recommend/tab/genre/{movie_id}")
async def recommend_genre(movie_id: int, limit: int = 5):
    check_model()
    recs = recommender.get_genre_recommendations(movie_id, ML_MODEL["df"], limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "genre_match"}

@app.get("/api/v1/recommend/tab/content/{movie_id}")
async def recommend_content(movie_id: int, limit: int = 5):
    check_model()
    recs = recommender.get_content_recommendations(movie_id, ML_MODEL["df"], ML_MODEL["similarity"], limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "content_based_similarity"}

@app.get("/api/v1/recommend/tab/hybrid/{movie_id}")
async def recommend_hybrid(movie_id: int, limit: int = 5):
    """Гибридный (совместимость с Java-бэкендом). Исправленные веса."""
    check_model()
    recs = recommender.get_hybrid_recommendations(movie_id, ML_MODEL["df"], ML_MODEL["similarity"], limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "hybrid_scoring"}

@app.get("/api/v1/recommend/tab/smart/{movie_id}")
async def recommend_smart(movie_id: int, limit: int = 5, user_id: int = None):
    """Умный гибрид: candidate generation → re-scoring → re-ranking. Исключает просмотренные."""
    check_model()
    recs = recommender.get_smart_hybrid_recommendations(
        movie_id, ML_MODEL["df"], ML_MODEL["similarity"], limit, user_id
    )
    if not recs:
        recs = recommender.get_hybrid_recommendations(movie_id, ML_MODEL["df"], ML_MODEL["similarity"], limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "smart_hybrid"}

@app.get("/api/v1/recommend/tab/collaborative-item/{movie_id}")
async def recommend_collaborative_item(movie_id: int, limit: int = 5):
    """Похожие пользователи также смотрели..."""
    recs = recommender.get_collaborative_users_also_watched(movie_id, limit)
    return {"movie_id": movie_id, "recommendations": recs, "method": "collaborative_item_based"}

@app.get("/api/v1/recommend/tab/domestic")
async def recommend_domestic(limit: int = 5):
    check_model()
    recs = recommender.get_domestic_recommendations(ML_MODEL["df"], limit)
    return {"recommendations": recs, "method": "domestic_cinema"}

@app.get("/api/v1/recommend/tab/because-you-liked/{user_id}")
async def recommend_because_you_liked(user_id: int, limit: int = 5):
    """Рекомендации на основе высоко оценённых фильмов пользователя (>= 7 баллов)"""
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

@app.get("/api/v1/recommend/collaborative/{user_id}")
async def recommend_collaborative_user(user_id: int, limit: int = 5):
    check_model()
    recs = recommender.get_collaborative_recommendations(user_id, ML_MODEL["df"], ML_MODEL["similarity"], limit)
    if not recs:
        recs = recommender.get_popular_fallback(ML_MODEL["df"], limit)
    return {"user_id": user_id, "recommendations": recs}

@app.get("/api/v1/recommend/feed/{user_id}")
async def get_smart_feed(user_id: int):
    check_model()
    feed = recommender.get_youtube_like_feed(user_id, ML_MODEL["df"], ML_MODEL["similarity"])
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
