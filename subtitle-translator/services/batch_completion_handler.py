import logging
import json
from openai import AsyncOpenAI
from services.checkpoint_repository import CheckpointRepository
from schemas.checkpoint import ChunkResult
from schemas.webhook import WebhookPayload

logger = logging.getLogger(__name__)


class BatchCompletionHandler:
    """
    Handles completed and failed OpenAI Batch jobs.
    Downloads results, saves chunks via CheckpointRepository,
    assembles the output file, and notifies Java backend.
    """

    def __init__(self, checkpoint_repo: CheckpointRepository, webhook_client, settings):
        self._repo = checkpoint_repo
        self._webhook_client = webhook_client
        self._api_key = settings.OPENAI_API_KEY

    async def handle_completion(self, batch_id: str) -> None:
        logger.info(f"Handling completion for batch {batch_id}")
        client = AsyncOpenAI(api_key=self._api_key)

        # 1. Retrieve Batch object
        batch = await client.batches.retrieve(batch_id)
        if not batch.output_file_id:
            logger.error(f"Batch {batch_id} completed but has no output_file_id")
            return

        # 2. Find Job in DB
        job = await self._repo.get_job_by_batch_id(batch_id)
        if not job:
            logger.error(f"No active job found for batch {batch_id}")
            return

        # 3. Download output .jsonl
        response = await client.files.content(batch.output_file_id)
        content = response.text

        # 4. Parse results and save chunks
        lines_translated = 0
        for line in content.strip().split("\n"):
            if not line:
                continue
            data = json.loads(line)
            custom_id = data.get("custom_id", "")

            try:
                response_content = data["response"]["body"]["choices"][0]["message"][
                    "content"
                ]
                text = response_content.strip()

                # Cleanup markdown fences
                if text.startswith("```"):
                    lines_split = text.split("\n")
                    if len(lines_split) >= 3:
                        text = "\n".join(lines_split[1:-1])
                    else:
                        text = text.replace("```json", "").replace("```", "")

                translated_lines = json.loads(text)
                translated_lines = [str(x) for x in translated_lines]

                chunk_index = (
                    job.batch_chunk_mapping.get(custom_id, 0)
                    if job.batch_chunk_mapping
                    else 0
                )

                chunk_res = ChunkResult(
                    chunk_index=chunk_index,
                    original_lines=[],
                    translated_lines=translated_lines,
                    model_used="openai-batch",
                    provider="openai",
                    attempt=1,
                )
                await self._repo.save_chunk(job.job_id, chunk_res)
                lines_translated += len(translated_lines)
                logger.info(
                    f"Saved batch chunk {custom_id} (index {chunk_index}) for job {job.job_id}"
                )

            except Exception as e:
                logger.error(
                    f"Failed to process batch chunk {custom_id} in batch {batch_id}: {e}"
                )

        # 5. Mark completed and send webhook
        await self._repo.update_job_status(job.job_id, "completed")
        logger.info(f"Job {job.job_id} completed via Batch!")

        await self._webhook_client.send_webhook(
            WebhookPayload(
                movie_id=job.movie_id,
                language="kk",
                status="success",
                output_path=job.output_path,
                lines_translated=lines_translated,
            )
        )

    async def handle_failure(self, batch_id: str, event_type: str) -> None:
        """Handle batch.failed / batch.expired / batch.cancelled events."""
        job = await self._repo.get_job_by_batch_id(batch_id)
        if not job:
            logger.error(f"No job found for failed batch {batch_id}")
            return

        await self._repo.update_job_status(job.job_id, "exhausted")
        logger.warning(f"Batch {batch_id} event: {event_type}. Job {job.job_id} exhausted.")

        await self._webhook_client.send_webhook(
            WebhookPayload(
                movie_id=job.movie_id,
                language="kk",
                status="partial",
                error_message=f"OpenAI Batch {event_type}",
            )
        )
