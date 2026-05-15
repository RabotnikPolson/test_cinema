import asyncio
from abc import ABC, abstractmethod
from typing import List
import logging
import json
from schemas.checkpoint import ChunkResult
from processing.tag_preservator import TagPreservator
from processing.translation_validator import TranslationValidator
from config.kazakh_prompt import VALIDATION_FIX_PROMPT

logger = logging.getLogger(__name__)

class RateLimitError(Exception): pass
class ServiceUnavailableError(Exception): pass
class AuthenticationError(Exception): pass
class LineMismatchError(Exception): pass
class BatchSubmittedSignal(BaseException):
    def __init__(self, batch_id: str, chunk_mapping: dict):
        self.batch_id = batch_id
        self.chunk_mapping = chunk_mapping

class BaseLLMProvider(ABC):
    def __init__(self, model: str, api_key: str, config: dict, tag_preservator: TagPreservator):
        self.model = model
        self.api_key = api_key
        self.config = config
        self._tag_preservator = tag_preservator
        self._max_retries = config.get("max_retries", 2)
        self.provider_name = config.get("provider", "unknown")

    async def translate_chunk(self, lines: List[str], context: str, movie_title: str, source_language: str = "ru", genre: str = "general", glossary: dict = None) -> ChunkResult:
        if glossary is None:
            glossary = {}
        expected_count = len(lines)
        
        # Pre-process: strip tags
        clean_lines, tag_map = self._tag_preservator.strip(lines)
        
        for attempt in range(1, self._max_retries + 1):
            try:
                result = await self._call_api(clean_lines, context, movie_title, source_language, genre, glossary)
                
                # STRICT LINE MATCHING
                if len(result) != expected_count:
                    logger.warning(
                        f"Line count mismatch: sent {expected_count}, "
                        f"got {len(result)} from {self.model} (attempt {attempt})"
                    )
                    if attempt < self._max_retries:
                        continue   # Retry same provider
                    raise LineMismatchError(
                        f"Persistent mismatch after {attempt} attempts. Expected {expected_count}, got {len(result)}."
                    )
                
                # Post-process: restore tags
                restored = self._tag_preservator.restore(result, tag_map)
                
                # Validation and retry logic
                validator = TranslationValidator(glossary=glossary)
                val_result = validator.validate(lines, restored)
                
                if not val_result.is_valid:
                    fix_instructions = VALIDATION_FIX_PROMPT.format(
                        issues=validator.build_fix_prompt_issues(val_result),
                        original_lines=json.dumps(lines, ensure_ascii=False),
                        broken_translation=json.dumps(restored, ensure_ascii=False)
                    )
                    logger.warning(f"Validation failed for chunk. Retrying once with fix_instructions...")
                    
                    try:
                        fixed_result = await self._call_api(clean_lines, context, movie_title, source_language, genre, glossary, fix_instructions=fix_instructions)
                        # We don't check line counts on the retry strictly to avoid dropping it entirely if it fails again, but we just restore tags.
                        # Wait, we should probably verify if the line counts match before restoring.
                        if len(fixed_result) == expected_count:
                            restored = self._tag_preservator.restore(fixed_result, tag_map)
                            val_result = validator.validate(lines, restored)
                    except Exception as e:
                        logger.error(f"Fix retry failed with exception: {e}")
                        
                    if not val_result.is_valid:
                        logger.error("Validation failed even after fix retry.")
                        for i in val_result.flagged_lines.keys():
                            restored[i] = f"[FIXME-EN] {restored[i]}"
                
                return ChunkResult(
                    chunk_index=-1, # Will be set by FallbackRouter
                    original_lines=lines,
                    translated_lines=restored,
                    model_used=self.model,
                    provider=self.provider_name,
                    attempt=attempt
                )
                
            except RateLimitError:
                if attempt < self._max_retries:
                    await asyncio.sleep(2 ** attempt)
                    continue
                raise   # -> FallbackRouter switches model
            except ServiceUnavailableError:
                raise   # -> FallbackRouter switches model
        
        raise LineMismatchError("Exhausted retries for line matching")

    @abstractmethod
    async def _call_api(self, lines: List[str], context: str, movie_title: str, source_language: str = "ru", genre: str = "general", glossary: dict = None, fix_instructions: str = None) -> List[str]:
        pass
