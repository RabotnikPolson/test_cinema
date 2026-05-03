import json
import logging
import tempfile
import os
from typing import List
from openai import AsyncOpenAI
from providers.base import BaseLLMProvider, RateLimitError, ServiceUnavailableError, AuthenticationError, BatchSubmittedSignal
from config.kazakh_prompt import TRANSLATION_PROMPT_TEMPLATE

logger = logging.getLogger(__name__)

class OpenAIProvider(BaseLLMProvider):
    def __init__(self, model: str, api_key: str, config: dict, tag_preservator, execution_mode: str = "standard"):
        super().__init__(model, api_key, config, tag_preservator)
        self.client = AsyncOpenAI(api_key=self.api_key)
        self.execution_mode = execution_mode

    async def _call_api(self, lines: List[str], context: str, movie_title: str) -> List[str]:
        prompt = TRANSLATION_PROMPT_TEMPLATE.format(
            movie_title=movie_title,
            context=context if context else "No context available.",
            lines=json.dumps(lines, ensure_ascii=False)
        )

        if self.execution_mode == "batch":
            return await self._execute_batch(prompt, len(lines))
        else:
            return await self._execute_standard(prompt)

    async def _execute_standard(self, prompt: str) -> List[str]:
        try:
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=[{"role": "user", "content": prompt}],
                temperature=self.config.get("temperature", 0.15)
            )
            text = response.choices[0].message.content.strip()
            
            if text.startswith("```"):
                lines_split = text.split("\n")
                if len(lines_split) >= 3:
                    text = "\n".join(lines_split[1:-1])
                else:
                    text = text.replace("```json", "").replace("```", "")
            
            translated = json.loads(text)
            if not isinstance(translated, list):
                raise ValueError(f"Expected a JSON list, got {type(translated)}")
                
            return [str(item) for item in translated]
            
        except Exception as e:
            self._handle_openai_error(e)

    async def _execute_batch(self, prompt: str, lines_count: int) -> List[str]:
        try:
            # Prepare a single JSONL line for this chunk
            # Custom ID "chunk_0" is just a placeholder; the Caller (TranslationService)
            # will map it to the actual chunk_index when it catches BatchSubmittedSignal.
            req = {
                "custom_id": "chunk_0",
                "method": "POST",
                "url": "/v1/chat/completions",
                "body": {
                    "model": self.model,
                    "messages": [{"role": "user", "content": prompt}],
                    "temperature": self.config.get("temperature", 0.15)
                }
            }
            
            with tempfile.NamedTemporaryFile(mode='w+', delete=False, suffix='.jsonl') as f:
                f.write(json.dumps(req) + "\n")
                temp_path = f.name
                
            with open(temp_path, "rb") as f:
                file_obj = await self.client.files.create(file=f, purpose="batch")
                
            os.remove(temp_path)
            
            batch = await self.client.batches.create(
                input_file_id=file_obj.id,
                endpoint="/v1/chat/completions",
                completion_window="24h"
            )
            
            logger.info(f"Successfully submitted OpenAI Batch. Batch ID: {batch.id}")
            raise BatchSubmittedSignal(batch.id, {"chunk_0": -1}) # -1 indicates the caller needs to replace it with the actual chunk_index
            
        except BatchSubmittedSignal:
            raise
        except Exception as e:
            self._handle_openai_error(e)

    def _handle_openai_error(self, e: Exception):
        error_msg = str(e).lower()
        if "429" in error_msg or "rate" in error_msg or "quota" in error_msg:
            raise RateLimitError(str(e))
        if "503" in error_msg or "500" in error_msg or "unavailable" in error_msg:
            raise ServiceUnavailableError(str(e))
        if "401" in error_msg or "403" in error_msg or "auth" in error_msg or "api key" in error_msg:
            raise AuthenticationError(str(e))
        raise e
