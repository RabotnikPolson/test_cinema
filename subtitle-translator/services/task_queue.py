import asyncio
import logging
from uuid import uuid4
from schemas.translation import WebhookPayload, TranslateRequest
from services.translation_service import TranslationService
from services.webhook_client import WebhookClient
from services.fallback_orchestrator import PartialTranslationError

logger = logging.getLogger(__name__)

translation_queue: asyncio.Queue = asyncio.Queue(maxsize=100)

async def worker_loop(translation_service: TranslationService, webhook_client: WebhookClient):
    """Единственный воркер. Читает задачи из очереди строго по одной."""
    logger.info("Translation worker started. Waiting for tasks...")
    while True:
        task_id, request = await translation_queue.get()
        try:
            logger.info(f"[{task_id}] Starting translation for movie {request.movie_id}")

            # Запускаем СИНХРОННЫЙ PySubtrans в отдельном потоке
            result = await asyncio.to_thread(
                translation_service.translate,
                input_path=request.input_path,
                output_path=request.output_path,
                movie_title=request.movie_title,
                source_language=request.source_language,
            )

            await webhook_client.send_webhook(WebhookPayload(
                movie_id=request.movie_id,
                language="kk",
                status="success",
                output_path=result.get("output_path"),
                lines_translated=result.get("lines_translated", 0),
            ))

        except PartialTranslationError as pe:
            logger.warning(
                f"[{task_id}] Partial success: {pe.lines_translated} lines translated. "
                f"Saved to {pe.output_path}"
            )
            await webhook_client.send_webhook(WebhookPayload(
                movie_id=request.movie_id,
                language="kk",
                status="partial",
                output_path=pe.output_path,
                lines_translated=pe.lines_translated,
                error_message=str(pe),
            ))
            
        except Exception as e:
            logger.error(f"[{task_id}] Translation failed completely: {e}")
            await webhook_client.send_webhook(WebhookPayload(
                movie_id=request.movie_id,
                language="kk",
                status="failed",
                error_message=str(e),
            ))
        finally:
            translation_queue.task_done()
