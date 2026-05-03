import logging
import asyncio
import httpx
import json
from datetime import datetime
import aiosqlite
from schemas.webhook import WebhookPayload
from services.checkpoint_repository import CheckpointRepository

logger = logging.getLogger(__name__)

class WebhookClient:
    def __init__(self, webhook_url: str, secret: str, checkpoint_repo: CheckpointRepository):
        self._url = webhook_url
        self._secret = secret
        self._repo = checkpoint_repo

    async def send_webhook(self, payload: WebhookPayload):
        async with httpx.AsyncClient(timeout=10.0) as client:
            for attempt in range(3):
                try:
                    resp = await client.post(
                        self._url, json=payload.model_dump(),
                        headers={"X-Internal-Secret": self._secret}
                    )
                    if resp.status_code in (200, 202, 204):
                        logger.info(f"Webhook delivered for movie {payload.movie_id}")
                        return
                    await asyncio.sleep(2 ** attempt)
                except httpx.RequestError as e:
                    logger.warning(f"Webhook attempt {attempt} failed: {e}")
                    await asyncio.sleep(2 ** attempt)

        logger.error(f"Webhook failed 3x for movie {payload.movie_id}. Persisting to retry queue.")
        async with aiosqlite.connect(self._repo._db_path) as db:
            await db.execute(
                "INSERT INTO pending_webhooks (payload, next_retry_at) VALUES (?, ?)",
                (json.dumps(payload.model_dump()), datetime.utcnow().isoformat())
            )
            await db.commit()
