from fastapi import APIRouter, Request, HTTPException
from openai import AsyncOpenAI
import os
import logging

logger = logging.getLogger(__name__)
router = APIRouter()

def get_batch_handler(request: Request):
    # Retrieve handler from FastAPI app state container
    return request.app.state.container.batch_handler

@router.post("/api/webhooks/openai")
async def openai_webhook(request: Request):
    payload = await request.body()
    
    # OpenAI webhooks typically use stripe-signature under the hood or custom headers
    # We pass the full headers object to the unwrap method
    secret = os.getenv("OPENAI_WEBHOOK_SECRET")
    api_key = os.getenv("OPENAI_API_KEY")
    
    if not secret or not api_key:
        logger.error("OPENAI_WEBHOOK_SECRET or OPENAI_API_KEY is missing")
        raise HTTPException(status_code=500, detail="Server misconfiguration")
        
    client = AsyncOpenAI(api_key=api_key)
    
    try:
        event = client.webhooks.unwrap(payload, dict(request.headers), secret=secret)
    except Exception as e:
        logger.error(f"Webhook signature verification failed: {e}")
        raise HTTPException(status_code=400, detail="Invalid signature")

    if event.type == "batch.completed":
        batch_id = event.data.object.id
        logger.info(f"Received batch.completed webhook for batch {batch_id}")
        
        handler = get_batch_handler(request)
        # Background process the completion to acknowledge the webhook quickly
        import asyncio
        asyncio.create_task(handler.handle_completion(batch_id))
        
    elif event.type in ("batch.failed", "batch.expired", "batch.cancelled"):
        batch_id = event.data.object.id
        logger.warning(f"Received {event.type} for batch {batch_id}")
        # In a real scenario, handle failure logic here
        
    return {"status": "received"}
