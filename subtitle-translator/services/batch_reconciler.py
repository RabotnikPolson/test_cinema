import asyncio
import logging
from openai import AsyncOpenAI
from services.checkpoint_repository import CheckpointRepository
from services.batch_completion_handler import BatchCompletionHandler

logger = logging.getLogger(__name__)

class BatchReconciler:
    """
    Safety net for lost webhooks. Checks periodically for stale awaiting_batch jobs.
    """
    STALE_THRESHOLD_MIN = 30
    POLL_INTERVAL_SEC = 300 # 5 minutes

    def __init__(self, checkpoint_repo: CheckpointRepository, batch_handler: BatchCompletionHandler, openai_api_key: str):
        self._repo = checkpoint_repo
        self._handler = batch_handler
        self._api_key = openai_api_key

    async def run_forever(self):
        logger.info("BatchReconciler started.")
        while True:
            await asyncio.sleep(self.POLL_INTERVAL_SEC)
            try:
                stale_jobs = await self._repo.get_stale_batch_jobs(self.STALE_THRESHOLD_MIN)
                if stale_jobs:
                    logger.info(f"Reconciler found {len(stale_jobs)} stale batch jobs. Checking statuses...")
                for job in stale_jobs:
                    await self._check_and_recover(job)
            except Exception as e:
                logger.error(f"BatchReconciler error: {e}")

    async def _check_and_recover(self, job):
        client = AsyncOpenAI(api_key=self._api_key)
        try:
            batch = await client.batches.retrieve(job.openai_batch_id)
        except Exception as e:
            logger.error(f"Failed to poll batch {job.openai_batch_id} for job {job.job_id}: {e}")
            return

        if batch.status == "completed":
            logger.warning(f"Reconciler: batch {batch.id} is 'completed' but stuck in DB. Triggering handler (webhook lost?).")
            await self._handler.handle_completion(batch.id)
        elif batch.status in ("failed", "expired", "cancelled"):
            logger.warning(f"Reconciler: batch {batch.id} status={batch.status}. Marking job as exhausted.")
            await self._repo.update_job_status(job.job_id, "exhausted")
        else:
            logger.info(f"Reconciler: batch {batch.id} is still {batch.status}.")

