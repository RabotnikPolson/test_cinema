import asyncio
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from dotenv import load_dotenv

# Нужно загрузить переменные окружения до импортов сервисов, 
# чтобы settings.py и webhook_client подхватили их
load_dotenv()

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

from services.translation_service import TranslationService
from services.webhook_client import WebhookClient
from services.task_queue import worker_loop
from routes.translate import router as translate_router

translation_service = TranslationService()
webhook_client = WebhookClient()

@asynccontextmanager
async def lifespan(app: FastAPI):
    # ── Startup ──
    task = asyncio.create_task(worker_loop(translation_service, webhook_client))
    logger.info("Translation worker task created")
    yield
    # ── Shutdown ──
    task.cancel()
    try:
        await task
    except asyncio.CancelledError:
        pass
    logger.info("Translation worker task cancelled")

app = FastAPI(title="Subtitle Translator API", lifespan=lifespan)

app.include_router(translate_router)
