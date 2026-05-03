import os
from config.settings import Settings
from services.checkpoint_repository import CheckpointRepository
from processing.tag_preservator import TagPreservator
from processing.smart_chunker import SmartChunker
from services.webhook_client import WebhookClient
from services.webhook_retry_worker import WebhookRetryWorker
from services.batch_completion_handler import BatchCompletionHandler
from services.batch_reconciler import BatchReconciler
from services.translation_service import TranslationService
from providers.gemini_provider import GeminiProvider
from providers.openai_provider import OpenAIProvider
from providers.base import BaseLLMProvider

class ServiceContainer:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.checkpoint_repo = CheckpointRepository(settings.CHECKPOINT_DB_PATH)
        self.tag_preservator = TagPreservator()
        self.smart_chunker = SmartChunker(
            gap_threshold_sec=settings.GAP_THRESHOLD_SEC,
            min_lines_per_chunk=settings.MIN_LINES_PER_CHUNK,
            max_lines_per_chunk=settings.MAX_LINES_PER_CHUNK,
        )
        self.webhook_client = WebhookClient(
            webhook_url=settings.JAVA_WEBHOOK_URL,
            secret=settings.INTERNAL_SECRET,
            checkpoint_repo=self.checkpoint_repo,
        )
        self.batch_handler = BatchCompletionHandler(
            checkpoint_repo=self.checkpoint_repo,
            webhook_client=self.webhook_client,
            settings=settings,
        )
        self.translation_service = TranslationService(
            checkpoint_repo=self.checkpoint_repo,
            smart_chunker=self.smart_chunker,
            provider_factory=self._build_providers,
            webhook_client=self.webhook_client,
            settings=settings,
        )
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

    def _build_providers(self, execution_mode: str) -> list[BaseLLMProvider]:
        providers = []
        
        if self.settings.GEMINI_API_KEY:
            providers.append(GeminiProvider(
                model="gemini-2.5-flash", 
                api_key=self.settings.GEMINI_API_KEY, 
                config={"max_retries": 2, "provider": "gemini"},
                tag_preservator=self.tag_preservator
            ))
            
        if self.settings.OPENAI_API_KEY:
            providers.append(OpenAIProvider(
                model="gpt-4o-mini",
                api_key=self.settings.OPENAI_API_KEY,
                config={"max_retries": 2, "provider": "openai"},
                tag_preservator=self.tag_preservator,
                execution_mode=execution_mode
            ))
            
        return providers

    async def startup(self):
        db_dir = os.path.dirname(self.settings.CHECKPOINT_DB_PATH)
        if db_dir and not os.path.exists(db_dir):
            os.makedirs(db_dir, exist_ok=True)
        await self.checkpoint_repo.initialize()

    async def shutdown(self):
        await self.checkpoint_repo.close()
