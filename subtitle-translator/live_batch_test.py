import asyncio
import os
import uuid
import json
from processing.tag_preservator import TagPreservator
from providers.openai_provider import OpenAIProvider
from services.checkpoint_repository import CheckpointRepository
from schemas.checkpoint import TranslationJob
from providers.base import BatchSubmittedSignal

# ==========================================
# 🚀 NGROK & OPENAI WEBHOOK INSTRUCTIONS
# ==========================================
# 1. Start ngrok to expose your local port 8100:
#    ngrok http 8100
#
# 2. Copy the forwarding URL from ngrok terminal (e.g., https://abcd-12-34-56-78.ngrok-free.app).
#
# 3. Go to OpenAI Dashboard -> API -> Webhooks (https://platform.openai.com/webhooks).
#    - Click "Add Endpoint".
#    - Endpoint URL: <YOUR_NGROK_URL>/api/webhooks/openai
#    - Select events to listen to: "batch.completed"
#    - Save and copy the "Signing Secret" (starts with whsec_...).
#
# 4. Set the following environment variables before running the FastAPI server:
#    export OPENAI_API_KEY="sk-..."
#    export OPENAI_WEBHOOK_SECRET="whsec_..."
#
# 5. Then run the FastAPI server (uvicorn main:app --port 8100) to catch the webhook!
# ==========================================

async def run_live_test():
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        print("ERROR: OPENAI_API_KEY environment variable not set.")
        print("Please set it (e.g., set OPENAI_API_KEY=sk-...) and try again.")
        return

    print("=== LIVE BATCH TEST ===")
    
    # 1. Init DB
    db_path = "live_test.db"
    if os.path.exists(db_path):
        os.remove(db_path)
    repo = CheckpointRepository(db_path)
    await repo.initialize()
    print("[+] DB Initialized: live_test.db")
    
    # 2. Create Job
    job = TranslationJob(
        job_id=str(uuid.uuid4()),
        movie_id=777,
        input_path="live.vtt",
        output_path="live_kk.vtt",
        movie_title="Live Action Movie",
        total_lines=6,
        chunk_size=6
    )
    await repo.create_job(job)
    print(f"[+] Job created. ID: {job.job_id}")
    
    # 3. Prepare real lines with tags
    lines = [
        "<i>Привет!</i>",
        "- Как дела?",
        "- Отлично, а у тебя?",
        "♪ Звучит эпичная музыка ♪",
        "Я уверен, мы победим.",
        "<b>Конец.</b>"
    ]
    
    # 4. Init Provider
    preservator = TagPreservator()
    provider = OpenAIProvider(
        model="gpt-4o-mini",
        api_key=api_key,
        config={"max_retries": 1, "temperature": 0.1},
        tag_preservator=preservator,
        execution_mode="batch"
    )
    
    # 5. Call translate_chunk
    print("[+] Submitting Batch to OpenAI...")
    try:
        await provider.translate_chunk(lines, "No context.", "Live Action Movie")
        print("[-] ERROR: translate_chunk did not raise BatchSubmittedSignal! It should have thrown it.")
    except BatchSubmittedSignal as signal:
        print(f"\n[+] SUCCESS! BatchSubmittedSignal raised.")
        print(f"    Batch ID: {signal.batch_id}")
        print(f"    Mapping: {signal.chunk_mapping}")
        
        # Update Job in DB
        job.openai_batch_id = signal.batch_id
        # We replace the -1 index from provider with the actual chunk index (0)
        actual_mapping = {"chunk_0": 0}
        
        import aiosqlite
        async with aiosqlite.connect(db_path) as db:
            await db.execute(
                "UPDATE translation_jobs SET status = 'awaiting_batch', openai_batch_id = ?, batch_chunk_mapping = ? WHERE job_id = ?",
                (signal.batch_id, json.dumps(actual_mapping), job.job_id)
            )
            await db.commit()
            
        print("[+] Job updated in DB to 'awaiting_batch' with batch ID.")
        print("\n🚀 Live submission completed successfully!")
        print("The batch is now processing on OpenAI servers. You can check its status on https://platform.openai.com/batches")
        print("\nTo test the webhook handler, run your FastAPI server with ngrok configured (see instructions in code) and wait for the ping!")

if __name__ == "__main__":
    asyncio.run(run_live_test())
