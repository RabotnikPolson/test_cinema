import asyncio
from pathlib import Path
from uuid import uuid4
from fastapi import APIRouter, HTTPException
from schemas.translation import TranslateRequest, TranslateResponse
from services.task_queue import translation_queue

router = APIRouter()

@router.get("/api/health")
async def healthcheck():
    return {"status": "healthy"}

@router.post("/api/translate", status_code=202, response_model=TranslateResponse)
async def translate_subtitle(request: TranslateRequest):
    # Быстрая валидация: существует ли файл?
    input_path = Path(request.input_path)
    if not input_path.exists():
        raise HTTPException(status_code=404, detail=f"Source file not found: {request.input_path}")

    task_id = str(uuid4())

    # Кладём в очередь (неблокирующий put)
    try:
        translation_queue.put_nowait((task_id, request))
    except asyncio.QueueFull:
        raise HTTPException(status_code=503, detail="Translation queue is full. Try again later.")

    return TranslateResponse(
        task_id=task_id,
        status="queued",
        position=translation_queue.qsize()
    )
