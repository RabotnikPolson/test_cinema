from pydantic import BaseModel
from typing import Optional

class TranslateRequest(BaseModel):
    movie_id: int
    input_path: str
    output_path: str
    movie_title: str
    source_language: str = "ru"
    execution_mode: Optional[str] = "standard"
