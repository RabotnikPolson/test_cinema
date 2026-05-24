from pydantic import BaseModel
from typing import Optional, List

class MovieRecommendation(BaseModel):
    id: int
    title: str
    poster_url: Optional[str] = None
    year: Optional[int] = None
    rating: Optional[float] = None
    genre: Optional[str] = None
    reason: Optional[str] = None

class RecommendationResponse(BaseModel):
    recommendations: List[MovieRecommendation]
    total: int
    method: Optional[str] = None
    movie_id: Optional[int] = None
    user_id: Optional[int] = None
    tab: Optional[str] = None

class ClickEvent(BaseModel):
    user_id: int
    movie_id: int
    source: Optional[str] = "unknown"

class SearchEvent(BaseModel):
    user_id: Optional[int] = None
    query: str

class SubtitleEvent(BaseModel):
    user_id: Optional[int] = None
    movie_id: int
    action: str

class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    movies_in_memory: int
    kazakhstan_movies: Optional[int] = 0
    version: str
    db_connected: Optional[bool] = True
