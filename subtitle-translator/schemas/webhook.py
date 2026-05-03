from pydantic import BaseModel
from typing import Optional

class WebhookPayload(BaseModel):
    movie_id: int
    language: str
    status: str
    output_path: Optional[str] = None
    lines_translated: Optional[int] = None
    error_message: Optional[str] = None
