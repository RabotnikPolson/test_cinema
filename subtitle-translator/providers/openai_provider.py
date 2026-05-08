import json
import logging
import tempfile
import os
from typing import List, Tuple, Dict
from openai import AsyncOpenAI
from providers.base import (
    BaseLLMProvider,
    RateLimitError,
    ServiceUnavailableError,
    AuthenticationError,
    BatchSubmittedSignal,
)
from config.kazakh_prompt import build_system_prompt, build_user_prompt

logger = logging.getLogger(__name__)


class OpenAIProvider(BaseLLMProvider):
    """
    OpenAI LLM Provider with two strategies:
    - 'standard': synchronous chat completions per chunk.
    - 'batch': used only via submit_batch_multi() from TranslationService.
    """

    def __init__(
        self,
        model: str,
        api_key: str,
        config: dict,
        tag_preservator,
        execution_mode: str = "standard",
    ):
        super().__init__(model, api_key, config, tag_preservator)
        self.client = AsyncOpenAI(api_key=self.api_key)
        self.execution_mode = execution_mode

    async def _call_api(
        self, lines: List[str], context: str, movie_title: str, source_language: str = "ru", genre: str = "general", glossary: dict = None
    ) -> List[str]:
        """Standard synchronous path. Always used per-chunk by FallbackRouter."""
        return await self._execute_standard(lines, context, movie_title, source_language, genre, glossary)

    async def _execute_standard(
        self, lines: List[str], context: str, movie_title: str, source_language: str = "ru", genre: str = "general", glossary: dict = None
    ) -> List[str]:
        sys_prompt = build_system_prompt(movie_title, source_language, genre, glossary)
        user_prompt = build_user_prompt(json.dumps(lines, ensure_ascii=False), context)
        try:
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": sys_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                temperature=self.config.get("temperature", 0.15),
            )
            text = response.choices[0].message.content.strip()
            return self._parse_json_response(text)
        except Exception as e:
            self._handle_openai_error(e)

    async def submit_batch_multi(
        self,
        chunks_data: List[Tuple[int, List[str], str, str]],
    ) -> Tuple[str, Dict[str, int]]:
        """
        Submit multiple chunks as a single OpenAI Batch job.

        Args:
            chunks_data: List of (chunk_index, cleaned_lines, context, movie_title).

        Returns:
            (batch_id, chunk_mapping) where chunk_mapping maps custom_id -> chunk_index.
        """
        chunk_mapping: Dict[str, int] = {}
        jsonl_lines: List[str] = []

        for chunk_index, lines, context, movie_title in chunks_data:
            custom_id = f"chunk_{chunk_index}"
            chunk_mapping[custom_id] = chunk_index

            sys_prompt = build_system_prompt(movie_title, source_lang="en", genre="general", glossary={})
            user_prompt = build_user_prompt(json.dumps(lines, ensure_ascii=False), context)

            req = {
                "custom_id": custom_id,
                "method": "POST",
                "url": "/v1/chat/completions",
                "body": {
                    "model": self.model,
                    "messages": [
                        {"role": "system", "content": sys_prompt},
                        {"role": "user", "content": user_prompt}
                    ],
                    "temperature": self.config.get("temperature", 0.15),
                },
            }
            jsonl_lines.append(json.dumps(req))

        # Write .jsonl to temp file, upload, submit batch
        with tempfile.NamedTemporaryFile(
            mode="w", delete=False, suffix=".jsonl", encoding="utf-8"
        ) as f:
            f.write("\n".join(jsonl_lines))
            temp_path = f.name

        try:
            with open(temp_path, "rb") as f:
                file_obj = await self.client.files.create(file=f, purpose="batch")

            batch = await self.client.batches.create(
                input_file_id=file_obj.id,
                endpoint="/v1/chat/completions",
                completion_window="24h",
            )
            logger.info(
                f"Submitted OpenAI Batch with {len(chunks_data)} chunks. "
                f"Batch ID: {batch.id}"
            )
            return batch.id, chunk_mapping
        finally:
            os.remove(temp_path)

    def _parse_json_response(self, text: str) -> List[str]:
        """Parse LLM JSON response, stripping markdown fences if present."""
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

    def _handle_openai_error(self, e: Exception):
        error_msg = str(e).lower()
        if "429" in error_msg or "rate" in error_msg or "quota" in error_msg:
            raise RateLimitError(str(e))
        if "503" in error_msg or "500" in error_msg or "unavailable" in error_msg:
            raise ServiceUnavailableError(str(e))
        if (
            "401" in error_msg
            or "403" in error_msg
            or "auth" in error_msg
            or "api key" in error_msg
        ):
            raise AuthenticationError(str(e))
        raise e
