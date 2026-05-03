from fastapi import APIRouter, BackgroundTasks, Request, HTTPException
from schemas.translation import TranslateRequest
import logging

router = APIRouter()
logger = logging.getLogger(__name__)

@router.post("/api/translate", status_code=202)
async def translate_endpoint(request: TranslateRequest, req: Request, background_tasks: BackgroundTasks):
    try:
        service = req.app.state.container.translation_service
        # Run translation task asynchronously in background
        background_tasks.add_task(service.translate, request)
        return {"status": "queued", "movie_id": request.movie_id}
    except Exception as e:
        logger.error(f"Error queuing translation: {e}")
        raise HTTPException(status_code=500, detail=str(e))
