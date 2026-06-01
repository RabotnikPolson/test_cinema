import asyncio
from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse
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
    settings = req.app.state.container.settings
    # Apply global execution mode from .env — Java doesn't send this field,
    # so OPENAI_EXECUTION_MODE="batch" in .env switches the entire pipeline to batch.
    request = request.model_copy(update={"execution_mode": settings.OPENAI_EXECUTION_MODE})

    task_queue: asyncio.Queue = req.app.state.task_queue
    await task_queue.put(request)
    logger.info(
        f"Translation request for movie {request.movie_id} queued "
        f"(mode={request.execution_mode}). Queue size: {task_queue.qsize()}"
    )
    return {"status": "queued", "movie_id": request.movie_id, "execution_mode": request.execution_mode}


@router.delete("/api/translate/{movie_id}", status_code=200)
async def cancel_translation(movie_id: int, req: Request):
    """
    Marks the active job for this movie as 'cancelled' in SQLite.
    The running worker checks this flag between chunks and stops gracefully.
    """
    container = req.app.state.container
    cancelled = await container.checkpoint_repo.cancel_active_job(movie_id)
    if cancelled:
        logger.info(f"Cancellation requested for movie {movie_id}. Job marked as cancelled.")
        return {"status": "cancelled", "movie_id": movie_id}
    return JSONResponse(
        status_code=404,
        content={"status": "not_found", "movie_id": movie_id, "detail": "No active job found"}
    )
