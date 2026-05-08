"""
Model chain configuration for the translation pipeline.

Gemini chain (FallbackRouter, synchronous, per-chunk):
  1. gemini-3.1-flash-lite-preview  — cheapest, fastest, lowest quality
  2. gemini-2.5-flash-lite          — cheap, fast
  3. gemini-3-flash                 — mid-tier
  4. gemini-2.5-flash               — highest quality Gemini

OpenAI fallback (Batch API, event-driven, all remaining chunks at once):
  - gpt-5-nano-2025-08-07          — triggered only when ALL Gemini models are exhausted
"""

GEMINI_CHAIN = [
    {
        "model": "gemini-3.1-flash-lite-preview",
        "provider": "gemini",
        "max_retries": 2,
        "temperature": 0.15,
    },
    {
        "model": "gemini-2.5-flash-lite",
        "provider": "gemini",
        "max_retries": 2,
        "temperature": 0.15,
    },
    {
        "model": "gemini-3-flash",
        "provider": "gemini",
        "max_retries": 2,
        "temperature": 0.15,
    },
    {
        "model": "gemini-2.5-flash",
        "provider": "gemini",
        "max_retries": 2,
        "temperature": 0.15,
    },
]

OPENAI_BATCH_MODEL = {
    "model": "gpt-5-nano-2025-08-07",
    "provider": "openai",
    "max_retries": 2,
    "reasoning_effort": "medium",
}
