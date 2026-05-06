import asyncio
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from dotenv import load_dotenv

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)

from config.settings import Settings
from services.container import ServiceContainer
from services.task_queue import worker_loop
from routes.translate import router as translate_router
from routes.openai_webhook import router as openai_webhook_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    # ── Startup ──
    settings = Settings()
    container = ServiceContainer(settings)
    await container.startup()

    app.state.container = container
    app.state.task_queue = asyncio.Queue()

    # Background tasks
    worker_task = asyncio.create_task(
        worker_loop(app.state.task_queue, container.translation_service)
    )
    webhook_retry_task = asyncio.create_task(
        container.webhook_retry_worker.run_forever()
    )
    batch_reconciler_task = asyncio.create_task(
        container.batch_reconciler.run_forever()
    )

    logger.info("Application startup complete. All background workers started.")

    yield

    # ── Shutdown ──
    logger.info("Application shutdown initiated.")
    for task in [worker_task, webhook_retry_task, batch_reconciler_task]:
        task.cancel()
    await asyncio.gather(
        worker_task, webhook_retry_task, batch_reconciler_task,
        return_exceptions=True,
    )
    await container.shutdown()
    logger.info("Application shutdown complete.")


app = FastAPI(title="Subtitle Translator API", lifespan=lifespan)
app.include_router(translate_router)
app.include_router(openai_webhook_router)
