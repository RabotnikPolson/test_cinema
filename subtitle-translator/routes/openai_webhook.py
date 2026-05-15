from fastapi import APIRouter, Request, HTTPException, BackgroundTasks
import logging

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post("/api/webhooks/openai")
async def openai_webhook(
    request: Request, background_tasks: BackgroundTasks
):
    """
    Receives OpenAI Batch completion webhooks.
    Validates signature via client.webhooks.unwrap using settings from DI container.
    Delegates heavy processing to BackgroundTasks to acknowledge the webhook quickly.
    """
    container = request.app.state.container
    settings = container.settings

    if not settings.OPENAI_WEBHOOK_SECRET or not settings.OPENAI_API_KEY:
        logger.error("OPENAI_WEBHOOK_SECRET or OPENAI_API_KEY is missing in settings.")
        raise HTTPException(status_code=500, detail="Server misconfiguration")

    payload = await request.body()

    from openai import AsyncOpenAI

    client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY)

    try:
        event = client.webhooks.unwrap(
            payload, dict(request.headers), secret=settings.OPENAI_WEBHOOK_SECRET
        )
    except Exception as e:
        logger.error(f"Webhook signature verification failed: {e}")
        raise HTTPException(status_code=400, detail="Invalid signature")

    handler = container.batch_handler

    if event.type == "batch.completed":
        batch_id = event.data.object.id
        logger.info(f"Received batch.completed webhook for batch {batch_id}")
        background_tasks.add_task(handler.handle_completion, batch_id)

    elif event.type in ("batch.failed", "batch.expired", "batch.cancelled"):
        batch_id = event.data.object.id
        logger.warning(f"Received {event.type} for batch {batch_id}")
        background_tasks.add_task(handler.handle_failure, batch_id, event.type)

    return {"status": "received"}
