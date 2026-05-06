import asyncio
import logging
from schemas.translation import TranslateRequest
from services.translation_service import TranslationService

logger = logging.getLogger(__name__)


async def worker_loop(
    task_queue: asyncio.Queue,
    translation_service: TranslationService,
) -> None:
    """
    Single sequential consumer. Pulls requests from the queue one at a time.
    This guarantees that we never hit RPM limits by running concurrent translations.
    """
    logger.info("Translation worker started. Waiting for tasks...")
    while True:
        try:
            request: TranslateRequest = await task_queue.get()
            logger.info(f"Worker picked up task for movie {request.movie_id}")

            try:
                result = await translation_service.translate(request)
                logger.info(f"Worker finished task for movie {request.movie_id}: {result}")
            except Exception as e:
                logger.error(
                    f"Worker: unhandled error for movie {request.movie_id}: {e}",
                    exc_info=True,
                )
            finally:
                task_queue.task_done()

        except asyncio.CancelledError:
            logger.info("Translation worker cancelled.")
            break
