import logging
import asyncio
import httpx
import json
from schemas.webhook import WebhookPayload
from services.checkpoint_repository import CheckpointRepository

logger = logging.getLogger(__name__)


class WebhookClient:
    """
    Sends webhooks to the Java backend.
    On failure after 3 fast retries, persists the payload to SQLite
    via CheckpointRepository for later retry by WebhookRetryWorker.
    """

    def __init__(
        self,
        webhook_url: str,
        secret: str,
        checkpoint_repo: CheckpointRepository,
    ):
        self._url = webhook_url
        self._secret = secret
        self._repo = checkpoint_repo

    async def send_webhook(self, payload: WebhookPayload) -> None:
        """3 fast retries. If all fail, persist to pending_webhooks via repository."""
        async with httpx.AsyncClient(timeout=10.0) as client:
            for attempt in range(3):
                try:
                    resp = await client.post(
                        self._url,
                        json=payload.model_dump(),
                        headers={"X-Internal-Secret": self._secret},
                    )
                    if resp.status_code in (200, 202, 204):
                        logger.info(
                            f"Webhook delivered for movie {payload.movie_id}"
                        )
                        return
                    if 400 <= resp.status_code < 500:
                        logger.error(
                            f"Webhook rejected with HTTP {resp.status_code} (Client Error). "
                            f"Movie ID {payload.movie_id} likely doesn't exist on Java side. Not retrying."
                        )
                        return
                    logger.warning(
                        f"Webhook attempt {attempt + 1} returned HTTP {resp.status_code}"
                    )
                except httpx.RequestError as e:
                    logger.warning(f"Webhook attempt {attempt + 1} failed: {e}")
                await asyncio.sleep(2**attempt)

        # All 3 fast retries failed — persist via repository
        logger.error(
            f"Webhook failed 3x for movie {payload.movie_id}. "
            "Persisting to retry queue."
        )
        await self._repo.enqueue_webhook(
            json.dumps(payload.model_dump(), ensure_ascii=False)
        )
