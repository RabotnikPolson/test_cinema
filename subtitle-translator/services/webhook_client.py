import httpx
import logging
import asyncio
from schemas.translation import WebhookPayload
import os

logger = logging.getLogger(__name__)

JAVA_WEBHOOK_URL = os.getenv("JAVA_WEBHOOK_URL", "http://localhost:8080/api/internal/subtitles/translation-complete")
INTERNAL_SECRET = os.getenv("INTERNAL_SECRET", "change-me-in-prod")

class WebhookClient:
    async def send_webhook(self, payload: WebhookPayload):
        """Отправляет результат перевода обратно на Java-бэкенд через httpx."""
        logger.info(f"Sending webhook for movie {payload.movie_id} with status {payload.status}")
        async with httpx.AsyncClient(timeout=10.0) as client:
            for attempt in range(3):
                try:
                    resp = await client.post(
                        JAVA_WEBHOOK_URL,
                        json=payload.model_dump(),
                        headers={"X-Internal-Secret": INTERNAL_SECRET}
                    )
                    if resp.status_code in (200, 202, 204):
                        logger.info(f"Webhook delivered successfully for movie {payload.movie_id}")
                        return
                    else:
                        logger.warning(f"Webhook returned status {resp.status_code}. Output: {resp.text}")
                        await asyncio.sleep(2 ** attempt)
                except httpx.RequestError as e:
                    logger.warning(f"Webhook attempt {attempt+1} failed: {e}")
                    await asyncio.sleep(2 ** attempt)

            logger.error(f"Failed to deliver webhook after 3 attempts for movie {payload.movie_id}")
