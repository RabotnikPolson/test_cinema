from pydantic import BaseModel
from typing import Optional, List, Dict
from datetime import datetime

class ChunkResult(BaseModel):
    chunk_index: int
    original_lines: List[str]
    translated_lines: List[str]
    model_used: str
    provider: str
    attempt: int
    translated_at: Optional[datetime] = None

class TranslationJob(BaseModel):
    job_id: str
    movie_id: int
    input_path: str
    output_path: str
    movie_title: str
    source_language: str = "ru"
    total_lines: int
    chunk_size: int
    next_chunk_index: int = 0
    status: str = "in_progress"
    openai_batch_id: Optional[str] = None
    batch_chunk_mapping: Optional[Dict[str, int]] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

class PendingWebhook(BaseModel):
    id: Optional[int] = None
    payload: str
    attempts: int = 0
    max_attempts: int = 15
    next_retry_at: datetime
    created_at: Optional[datetime] = None
    last_error: Optional[str] = None
