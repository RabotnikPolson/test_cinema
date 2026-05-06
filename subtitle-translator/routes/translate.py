import asyncio
from fastapi import APIRouter, Request
from schemas.translation import TranslateRequest
import logging

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post("/api/translate", status_code=202)
async def translate_endpoint(request: TranslateRequest, req: Request):
    """
    Accepts a translation request and places it into the global asyncio.Queue.
    The single worker consumer (started in main.py lifespan) will process
    requests sequentially, preventing RPM limit overruns.
    """
    task_queue: asyncio.Queue = req.app.state.task_queue
    await task_queue.put(request)
    logger.info(
        f"Translation request for movie {request.movie_id} queued. "
        f"Queue size: {task_queue.qsize()}"
    )
    return {"status": "queued", "movie_id": request.movie_id}
