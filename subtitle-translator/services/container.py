import os
from typing import List, Optional
from config.settings import Settings
from config.models import GEMINI_CHAIN, OPENAI_BATCH_MODEL
from services.checkpoint_repository import CheckpointRepository
from processing.tag_preservator import TagPreservator
from processing.smart_chunker import SmartChunker
from services.webhook_client import WebhookClient
from services.webhook_retry_worker import WebhookRetryWorker
from services.batch_completion_handler import BatchCompletionHandler
from services.batch_reconciler import BatchReconciler
from services.translation_service import TranslationService
from providers.base import BaseLLMProvider


class ServiceContainer:
    """
    Composition Root (Dependency Injection).

    Architecture:
      - FallbackRouter receives ONLY Gemini providers (sync, per-chunk).
      - OpenAI provider is built separately and used ONLY for batch fallback
        when ALL Gemini models are exhausted.
    """

    def __init__(self, settings: Settings):
        self.settings = settings

        # Infrastructure
        self.checkpoint_repo = CheckpointRepository(settings.CHECKPOINT_DB_PATH)
        self.tag_preservator = TagPreservator()
        self.smart_chunker = SmartChunker(
            gap_threshold_sec=settings.GAP_THRESHOLD_SEC,
            min_lines_per_chunk=settings.MIN_LINES_PER_CHUNK,
            max_lines_per_chunk=settings.MAX_LINES_PER_CHUNK,
        )

        # Webhook layer
        self.webhook_client = WebhookClient(
            webhook_url=settings.JAVA_WEBHOOK_URL,
            secret=settings.INTERNAL_SECRET,
            checkpoint_repo=self.checkpoint_repo,
        )

        # Batch handler
        self.batch_handler = BatchCompletionHandler(
            checkpoint_repo=self.checkpoint_repo,
            webhook_client=self.webhook_client,
            settings=settings,
        )

        # Translation service
        self.translation_service = TranslationService(
            checkpoint_repo=self.checkpoint_repo,
            smart_chunker=self.smart_chunker,
            tag_preservator=self.tag_preservator,
            gemini_provider_factory=self._build_gemini_chain,
            openai_provider_factory=self._build_openai_batch_provider,
            webhook_client=self.webhook_client,
            settings=settings,
        )

        # Background workers
        self.webhook_retry_worker = WebhookRetryWorker(
            checkpoint_repo=self.checkpoint_repo,
            webhook_url=settings.JAVA_WEBHOOK_URL,
            secret=settings.INTERNAL_SECRET,
        )
        self.batch_reconciler = BatchReconciler(
            checkpoint_repo=self.checkpoint_repo,
            batch_handler=self.batch_handler,
            openai_api_key=settings.OPENAI_API_KEY,
        )

    def _build_gemini_chain(self) -> List[BaseLLMProvider]:
        """Build the Gemini-only provider chain for FallbackRouter."""
        if not self.settings.GEMINI_API_KEY:
            return []

        from providers.gemini_provider import GeminiProvider

        providers = []
        for cfg in GEMINI_CHAIN:
            providers.append(
                GeminiProvider(
                    model=cfg["model"],
                    api_key=self.settings.GEMINI_API_KEY,
                    config=cfg,
                    tag_preservator=self.tag_preservator,
                )
            )
        return providers

    def _build_openai_batch_provider(self):
        """Build the OpenAI provider for batch-only fallback."""
        if not self.settings.OPENAI_API_KEY:
            return None

        from providers.openai_provider import OpenAIProvider

        return OpenAIProvider(
            model=OPENAI_BATCH_MODEL["model"],
            api_key=self.settings.OPENAI_API_KEY,
            config=OPENAI_BATCH_MODEL,
            tag_preservator=self.tag_preservator,
            execution_mode="batch",
        )

    async def startup(self) -> None:
        db_dir = os.path.dirname(self.settings.CHECKPOINT_DB_PATH)
        if db_dir and not os.path.exists(db_dir):
            os.makedirs(db_dir, exist_ok=True)
        await self.checkpoint_repo.initialize()

    async def shutdown(self) -> None:
        await self.checkpoint_repo.close()
