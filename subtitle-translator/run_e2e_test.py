import asyncio
import httpx
import os

async def run_e2e_test():
    print("=== E2E API TEST ===")
    print("Make sure `uvicorn main:app --port 8100` is running in another terminal!\n")
    
    if not os.path.exists("./storage"):
        os.makedirs("./storage")
    
    input_path = "./storage/test_input.vtt"
    with open(input_path, "w", encoding="utf-8") as f:
        f.write("WEBVTT\n\n00:00:00.000 --> 00:00:02.000\n<i>Привет!</i>\n\n")
        f.write("00:00:02.500 --> 00:00:04.000\n- Как дела?\n- Нормально.\n\n")
        f.write("00:00:10.000 --> 00:00:12.000\n♪ Звучит музыка ♪\n\n")
        f.write("00:00:12.500 --> 00:00:15.000\n<b>Конец.</b>\n\n")
        
    payload = {
        "movie_id": 999,
        "input_path": input_path,
        "output_path": "./storage/test_output.vtt",
        "movie_title": "E2E Movie",
        "source_language": "ru",
        # Set execution_mode to "standard" to test Gemini (sync) or "batch" to test OpenAI Batch (async)
        "execution_mode": "batch" 
    }
    
    print(f"Sending translation request for movie_id {payload['movie_id']} in {payload['execution_mode']} mode...")
    
    try:
        async with httpx.AsyncClient() as client:
            resp = await client.post("http://localhost:8100/api/translate", json=payload)
            print(f"\nStatus: {resp.status_code}")
            print(f"Response: {resp.json()}")
            
            if resp.status_code == 202:
                print("\n✅ Successfully queued translation task!")
                print("Look at the uvicorn terminal to see logs of the TranslationService running.")
                print("If mode is 'batch', it will suspend and wait for OpenAI webhook.")
            else:
                print("\n❌ Failed to queue task.")
    except Exception as e:
        print(f"\n❌ Error connecting to server: {e}")
        print("Did you start the FastAPI server on port 8100?")

if __name__ == "__main__":
    asyncio.run(run_e2e_test())
