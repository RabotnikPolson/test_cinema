from pydantic import BaseModel
from typing import Optional, Literal

class TranslateRequest(BaseModel):
    movie_id: int
    input_path: str
    output_path: str
    movie_title: str
    source_language: str = "ru"

class TranslateResponse(BaseModel):
    task_id: str
    status: str
    position: int

class WebhookPayload(BaseModel):
    movie_id: int
    language: str
    status: Literal["success", "partial", "failed"]
    output_path: Optional[str] = None
    lines_translated: Optional[int] = 0
    error_message: Optional[str] = None
