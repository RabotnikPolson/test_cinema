import asyncio
import logging
import httpx
import json
from services.checkpoint_repository import CheckpointRepository

logger = logging.getLogger(__name__)


class WebhookRetryWorker:
    """
    Background asyncio task. Polls pending_webhooks every 30 seconds.
    Exponential backoff: 1m → 2m → 4m → … → cap 1h. Max 15 attempts ≈ 24h.
    Dead letters are marked status='failed', NOT deleted (Audit Trail).
    """

    def __init__(
        self,
        checkpoint_repo: CheckpointRepository,
        webhook_url: str,
        secret: str,
    ):
        self._repo = checkpoint_repo
        self._url = webhook_url
        self._secret = secret

    async def run_forever(self) -> None:
        logger.info("WebhookRetryWorker started.")
        while True:
            await asyncio.sleep(30)
            try:
                pending = await self._repo.get_due_webhooks()
                for webhook in pending:
                    await self._process_webhook(webhook)
            except Exception as e:
                logger.error(f"WebhookRetryWorker cycle error: {e}")

    async def _process_webhook(self, webhook) -> None:
        payload_dict = json.loads(webhook.payload)
        success = False
        last_err = ""

        async with httpx.AsyncClient(timeout=10.0) as client:
            try:
                resp = await client.post(
                    self._url,
                    json=payload_dict,
                    headers={"X-Internal-Secret": self._secret},
                )
                if resp.status_code in (200, 202, 204):
                    success = True
                elif 400 <= resp.status_code < 500:
                    logger.critical(
                        f"DEAD LETTER: webhook {webhook.id} rejected with HTTP {resp.status_code}. "
                        f"Payload: {webhook.payload}. Marking as 'failed'."
                    )
                    await self._repo.mark_webhook_failed(webhook.id)
                    return
                else:
                    last_err = f"HTTP {resp.status_code}"
            except Exception as e:
                last_err = str(e)

        if success:
            await self._repo.delete_webhook(webhook.id)
            logger.info(f"Successfully retried webhook {webhook.id}")
        else:
            new_attempts = webhook.attempts + 1
            if new_attempts >= webhook.max_attempts:
                logger.critical(
                    f"DEAD LETTER: webhook {webhook.id} failed {new_attempts} times. "
                    f"Payload: {webhook.payload}. Marking as 'failed'."
                )
                await self._repo.mark_webhook_failed(webhook.id)
            else:
                next_delay = min(60 * (2**new_attempts), 3600)
                await self._repo.bump_webhook_retry(
                    webhook.id, next_delay, last_err
                )
                logger.warning(
                    f"Webhook {webhook.id} retry #{new_attempts} failed. "
                    f"Next retry in {next_delay}s."
                )
