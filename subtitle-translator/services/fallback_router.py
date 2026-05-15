import logging
from typing import List
from providers.base import BaseLLMProvider, RateLimitError, ServiceUnavailableError, AuthenticationError, LineMismatchError, BatchSubmittedSignal
from schemas.checkpoint import ChunkResult

logger = logging.getLogger(__name__)

class ExhaustedProvidersError(Exception):
    def __init__(self, chunk_index: int):
        super().__init__(f"All providers exhausted for chunk {chunk_index}")
        self.chunk_index = chunk_index

class FallbackRouter:
    def __init__(self, provider_chain: List[BaseLLMProvider]):
        self._active_providers = list(provider_chain)

    async def translate_chunk(self, lines: List[str], context: str, movie_title: str, chunk_index: int, source_language: str = "ru", genre: str = "general", glossary: dict = None) -> ChunkResult:
        for provider in self._active_providers:
            try:
                logger.info(f"Trying provider {provider.model} for chunk {chunk_index}...")
                result = await provider.translate_chunk(lines, context, movie_title, source_language, genre, glossary)
                result.chunk_index = chunk_index
                return result
            except (RateLimitError, ServiceUnavailableError, LineMismatchError) as e:
                logger.warning(f"Chunk {chunk_index}: {provider.model} failed ({type(e).__name__}: {e}), trying next...")
                continue
            except AuthenticationError as e:
                logger.error(f"Chunk {chunk_index}: {provider.model} Authentication Error. Removing from active chain.")
                self._active_providers.remove(provider)
                continue
            except BatchSubmittedSignal:
                # This is a signal to the TranslationService, propagate it up
                raise
        
        logger.error(f"Exhausted all active providers for chunk {chunk_index}.")
        raise ExhaustedProvidersError(chunk_index)
