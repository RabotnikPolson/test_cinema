import asyncio
import logging
import httpx
import json
import aiosqlite
from services.checkpoint_repository import CheckpointRepository

logger = logging.getLogger(__name__)

class WebhookRetryWorker:
    def __init__(self, checkpoint_repo: CheckpointRepository, webhook_url: str, secret: str):
        self._repo = checkpoint_repo
        self._url = webhook_url
        self._secret = secret

    async def run_forever(self):
        logger.info("WebhookRetryWorker started.")
        while True:
            await asyncio.sleep(30)
            try:
                async with aiosqlite.connect(self._repo._db_path) as db:
                    db.row_factory = aiosqlite.Row
                    async with db.execute("SELECT * FROM pending_webhooks WHERE next_retry_at <= datetime('now')") as cursor:
                        pending = await cursor.fetchall()
                        
                for row in pending:
                    await self._process_webhook(row)
            except Exception as e:
                logger.error(f"WebhookRetryWorker error: {e}")

    async def _process_webhook(self, row):
        webhook_id = row["id"]
        payload_dict = json.loads(row["payload"])
        attempts = row["attempts"]
        
        success = False
        last_err = ""
        async with httpx.AsyncClient(timeout=10.0) as client:
            try:
                resp = await client.post(
                    self._url, json=payload_dict,
                    headers={"X-Internal-Secret": self._secret}
                )
                if resp.status_code in (200, 202, 204):
                    success = True
                else:
                    last_err = f"HTTP {resp.status_code}"
            except Exception as e:
                last_err = str(e)

        async with aiosqlite.connect(self._repo._db_path) as db:
            if success:
                await db.execute("DELETE FROM pending_webhooks WHERE id = ?", (webhook_id,))
                logger.info(f"Successfully retried webhook {webhook_id}")
            else:
                new_attempts = attempts + 1
                if new_attempts >= row["max_attempts"]:
                    logger.critical(f"DEAD LETTER: webhook {webhook_id} failed {new_attempts} times.")
                    await db.execute("DELETE FROM pending_webhooks WHERE id = ?", (webhook_id,))
                else:
                    next_delay = min(60 * (2 ** new_attempts), 3600)
                    await db.execute(
                        "UPDATE pending_webhooks SET attempts = ?, next_retry_at = datetime('now', ?), last_error = ? WHERE id = ?",
                        (new_attempts, f"+{next_delay} seconds", last_err, webhook_id)
                    )
            await db.commit()
