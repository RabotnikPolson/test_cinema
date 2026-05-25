from pydantic import BaseModel
from typing import Optional, List

class MovieRecommendation(BaseModel):
    movie_id: Optional[int] = None
    id: Optional[int] = None
    title: Optional[str] = None
    poster_url: Optional[str] = None
    year: Optional[int] = None
    score: Optional[float] = None
    rating: Optional[float] = None
    genre: Optional[str] = None
    reason: Optional[str] = None

    model_config = {"extra": "allow"}

class RecommendationResponse(BaseModel):
    recommendations: List[MovieRecommendation]
    total: int
    method: Optional[str] = None
    movie_id: Optional[int] = None
    user_id: Optional[int] = None
    tab: Optional[str] = None

class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    movies_in_memory: int
    kazakhstan_movies: Optional[int] = 0
    version: str
    db_connected: Optional[bool] = True
