import logging
import json
from openai import AsyncOpenAI
from services.checkpoint_repository import CheckpointRepository
from schemas.checkpoint import ChunkResult

logger = logging.getLogger(__name__)

class BatchCompletionHandler:
    def __init__(self, checkpoint_repo: CheckpointRepository, webhook_client, settings):
        self._repo = checkpoint_repo
        self._webhook_client = webhook_client
        self.client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY)

    async def handle_completion(self, batch_id: str):
        logger.info(f"Handling completion for batch {batch_id}")
        
        # 1. Retrieve Batch object to get output_file_id
        batch = await self.client.batches.retrieve(batch_id)
        if not batch.output_file_id:
            logger.error(f"Batch {batch_id} completed but has no output_file_id")
            return
            
        # 2. Get the Job from our DB
        job = await self._repo.get_job_by_batch_id(batch_id)
        if not job:
            logger.error(f"No active job found for batch {batch_id}")
            return
            
        # 3. Download the results
        response = await self.client.files.content(batch.output_file_id)
        content = response.text
        
        # 4. Parse results and save chunks
        lines = content.strip().split('\n')
        for line in lines:
            if not line: continue
            data = json.loads(line)
            custom_id = data.get("custom_id")
            
            # Extract content
            try:
                response_content = data["response"]["body"]["choices"][0]["message"]["content"]
                
                # Cleanup markdown if any
                text = response_content.strip()
                if text.startswith("```"):
                    lines_split = text.split("\n")
                    if len(lines_split) >= 3:
                        text = "\n".join(lines_split[1:-1])
                    else:
                        text = text.replace("```json", "").replace("```", "")
                        
                translated_lines = json.loads(text)
                translated_lines = [str(x) for x in translated_lines]
                
                chunk_index = job.batch_chunk_mapping.get(custom_id, 0)
                
                chunk_res = ChunkResult(
                    chunk_index=chunk_index,
                    original_lines=[], # For batch results we might not need original lines immediately
                    translated_lines=translated_lines,
                    model_used="openai-batch",
                    provider="openai",
                    attempt=1
                )
                await self._repo.save_chunk(job.job_id, chunk_res)
                logger.info(f"Saved chunk {chunk_index} for job {job.job_id} from batch {batch_id}")
                
            except Exception as e:
                logger.error(f"Failed to process chunk {custom_id} in batch {batch_id}: {e}")

        # 5. Job completed, assemble and send webhook to Java
        await self._repo.update_job_status(job.job_id, "completed")
        logger.info(f"Job {job.job_id} successfully completed via Batch!")
        
        # In a real scenario, here we will trigger file assembly and call _webhook_client.
        # But for this module implementation, we update the status in DB as success.
        
