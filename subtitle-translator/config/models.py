# ═══════════════════════════════════════════════════════════════
# ГЛОБАЛЬНЫЕ ПАРАМЕТРЫ БАТЧИНГА — применяются ОДИН РАЗ при init_subtitles()
# Эти значения НЕ могут меняться при fallback-переключении модели.
# ═══════════════════════════════════════════════════════════════
BATCHING_CONFIG = {
    "max_batch_size": 80,      # Наименьший общий знаменатель всех моделей
    "scene_threshold": 90.0,   # Порог разбиения на сцены (секунды паузы)
}

# ═══════════════════════════════════════════════════════════════
# ЦЕПОЧКА МОДЕЛЕЙ — только провайдер/модель/auth-параметры.
# БЕЗ max_batch_size — батчинг не зависит от модели.
# ═══════════════════════════════════════════════════════════════
FALLBACK_CHAIN = [
    {
        "provider": "Gemini",
        "model": "gemini-3.1-flash-lite-preview",
        "api_key_env": "GEMINI_API_KEY",
        "rate_limit": 10.0,
        "temperature": 0.15,
        "stream_responses": False,
    },
    {
        "provider": "Gemini",
        "model": "gemini-3-flash-preview",
        "api_key_env": "GEMINI_API_KEY",
        "rate_limit": 10.0,
        "temperature": 0.15,
        "stream_responses": False,
    },
]
