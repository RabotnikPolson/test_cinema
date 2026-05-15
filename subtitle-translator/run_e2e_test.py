import asyncio
import httpx
import os

# ═══════════════════════════════════════════════════════════════
#  E2E API TEST
#
#  PREREQUISITES:
#    1. pip install pydantic-settings aiosqlite httpx pysubtrans openai google-generativeai python-dotenv
#    2. Create a .env file in subtitle-translator/ with at least:
#       GEMINI_API_KEY=your_key_here
#    3. Start the server:
#       uvicorn main:app --port 8100
#    4. In a separate terminal, run this script:
#       python run_e2e_test.py
#
#  WHAT THIS SCRIPT DOES:
#    - Creates a properly formatted .vtt subtitle file with tags and timecodes
#    - Sends a POST /api/translate request to the running server
#    - The server queues it, the worker picks it up, translates via Gemini,
#      saves chunks to SQLite, assembles the output file, and sends a webhook
#    - You watch the uvicorn terminal for real-time logs
# ═══════════════════════════════════════════════════════════════

VTT_CONTENT = """WEBVTT

00:00:01.000 --> 00:00:03.500
<i>Здравствуйте, дорогие зрители!</i>

00:00:04.000 --> 00:00:06.000
- Как ваши дела?
- Отлично, спасибо.

00:00:06.500 --> 00:00:09.000
Сегодня мы расскажем вам невероятную историю.

00:00:15.000 --> 00:00:17.500
♪ Звучит эпичная музыка ♪

00:00:18.000 --> 00:00:20.000
Давным-давно, в далёком королевстве...

00:00:20.500 --> 00:00:23.000
...жил один <b>храбрый</b> воин.

00:00:23.500 --> 00:00:26.000
Он мечтал о свободе для своего народа.

00:00:32.000 --> 00:00:34.500
Но враги были повсюду.

00:00:35.000 --> 00:00:37.000
- Кто ты такой?
- Я — защитник этих земель.

00:00:37.500 --> 00:00:40.000
Ты не пройдёшь дальше.

00:00:40.500 --> 00:00:43.000
<i>И тогда началась великая битва...</i>
"""


async def main():
    print("=" * 60)
    print("  E2E SUBTITLE TRANSLATION TEST")
    print("=" * 60)

    # 1. Create test subtitle file
    storage_dir = "./storage/subtitles/999"
    os.makedirs(storage_dir, exist_ok=True)
    input_path = os.path.join(storage_dir, "ru.vtt")
    output_path = os.path.join(storage_dir, "kk.vtt")

    with open(input_path, "w", encoding="utf-8") as f:
        f.write(VTT_CONTENT)
    print(f"\n[+] Created test VTT file: {input_path}")
    print(f"    Lines: 12 subtitle entries with <i>, <b>, ♪, and dialogue dashes")

    # 2. Send translation request
    payload = {
        "movie_id": 999,
        "input_path": input_path,
        "output_path": output_path,
        "movie_title": "Батыр: Степной Легенда",
        "source_language": "ru",
        "execution_mode": "standard",
    }

    print(f"\n[+] Sending POST /api/translate...")
    print(f"    movie_id: {payload['movie_id']}")
    print(f"    mode: {payload['execution_mode']}")

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(
                "http://localhost:8100/api/translate", json=payload
            )
            print(f"\n    Status Code: {resp.status_code}")
            print(f"    Response: {resp.json()}")

            if resp.status_code == 202:
                print("\n" + "=" * 60)
                print("  ✅ REQUEST QUEUED SUCCESSFULLY!")
                print("=" * 60)
                print(
                    "\n  Now watch the uvicorn terminal for real-time translation logs."
                )
                print("  When finished, check the output file:")
                print(f"    {output_path}")
                print(
                    "\n  The server will also attempt to send a webhook to Java backend."
                )
                print(
                    "  If Java is not running, the webhook will be persisted for retry."
                )
            else:
                print(f"\n Unexpected status code: {resp.status_code}")

    except httpx.ConnectError:
        print("\n  Cannot connect to http://localhost:8100")
        print("  Did you start the server? Run: uvicorn main:app --port 8100")
    except Exception as e:
        print(f"\n  Error: {e}")


if __name__ == "__main__":
    asyncio.run(main())
