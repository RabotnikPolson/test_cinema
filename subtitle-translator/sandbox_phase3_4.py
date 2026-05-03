import asyncio
import logging
from processing.tag_preservator import TagPreservator
from providers.base import BaseLLMProvider, RateLimitError, LineMismatchError, ServiceUnavailableError
from services.fallback_router import FallbackRouter

# Setup basic logging to see the logger outputs
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")

# --- MOCKS ---

class MockGeminiFlashLite(BaseLLMProvider):
    def __init__(self, tag_preservator):
        super().__init__("gemini-3.1-flash-lite-preview", "fake_key", {"max_retries": 2, "provider": "gemini"}, tag_preservator)
        self.call_count = 0

    async def _call_api(self, lines, context, movie_title):
        self.call_count += 1
        print(f"    [MockGeminiFlashLite] _call_api invoked (Attempt {self.call_count}). Raising RateLimitError...")
        raise RateLimitError("Quota exceeded (429)")

class MockGeminiFlash(BaseLLMProvider):
    def __init__(self, tag_preservator):
        super().__init__("gemini-3-flash-preview", "fake_key", {"max_retries": 2, "provider": "gemini"}, tag_preservator)
        self.call_count = 0

    async def _call_api(self, lines, context, movie_title):
        self.call_count += 1
        print(f"    [MockGeminiFlash] _call_api invoked (Attempt {self.call_count}). Returning mismatching lines count...")
        # Returning only 1 line instead of 2
        return ["<0>Жалғыз жол<1>"]

class MockGemini25Flash(BaseLLMProvider):
    def __init__(self, tag_preservator):
        super().__init__("gemini-2.5-flash", "fake_key", {"max_retries": 2, "provider": "gemini"}, tag_preservator)

    async def _call_api(self, lines, context, movie_title):
        print("    [MockGemini25Flash] _call_api invoked. Returning correct translation...")
        return [
            "<0>Сәлем!<1>",
            "<2>Қалайсың?<3><4>Қалыпты."
        ]

# --- SANDBOX EXECUTION ---

async def run_sandbox():
    print("=== SANDBOX: Phase 3 & 4 (Fallback Router & BaseLLMProvider) ===\n")
    
    tag_preservator = TagPreservator()
    
    # Setup chain
    provider1 = MockGeminiFlashLite(tag_preservator)
    provider2 = MockGeminiFlash(tag_preservator)
    provider3 = MockGemini25Flash(tag_preservator)
    
    router = FallbackRouter(provider_chain=[provider1, provider2, provider3])
    
    # Fake chunk (2 lines)
    chunk_lines = [
        "<i>Привет!</i>",
        "- Как дела?\n- Нормально."
    ]
    
    print(f"Input Chunk (2 lines):")
    for l in chunk_lines:
        print(f"  {l}")
    print("\n--- Starting Translation via FallbackRouter ---\n")
    
    try:
        result = await router.translate_chunk(
            lines=chunk_lines,
            context="<Previous context>",
            movie_title="Sandbox Movie",
            chunk_index=42
        )
        
        print("\n--- Translation Successful! ---")
        print(f"Model Used: {result.model_used}")
        print(f"Provider: {result.provider}")
        print(f"Attempt: {result.attempt}")
        print(f"Output Lines:")
        for l in result.translated_lines:
            print(f"  {l}")
            
    except Exception as e:
        print(f"\nTranslation Failed completely: {e}")

if __name__ == "__main__":
    asyncio.run(run_sandbox())
