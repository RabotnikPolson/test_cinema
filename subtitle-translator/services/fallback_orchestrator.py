import os
import logging
from PySubtrans import init_options, init_translation_provider, SubtitleTranslator

from config.models import BATCHING_CONFIG, FALLBACK_CHAIN
from config.kazakh_prompt import KAZAKH_USER_PROMPT, KAZAKH_SYSTEM_INSTRUCTIONS

logger = logging.getLogger(__name__)

class PartialTranslationError(Exception):
    def __init__(self, message: str, output_path: str, lines_translated: int, original_error: Exception = None):
        super().__init__(message)
        self.output_path = output_path
        self.lines_translated = lines_translated
        self.original_error = original_error

class FallbackOrchestrator:
    def execute_with_fallback(self, subtitles, movie_title: str, output_path: str):
        last_exception = None

        for model_config in FALLBACK_CHAIN:
            provider_name = model_config["provider"]
            
            api_key = os.getenv(model_config["api_key_env"])
            if not api_key:
                logger.warning(f"Skipping {model_config['model']}: {model_config['api_key_env']} is not set.")
                continue

            # БЕЗ max_batch_size и scene_threshold (они уже применены в init_subtitles)
            options = dict(
                provider=provider_name,
                model=model_config["model"],
                api_key=api_key,
                target_language="Kazakh",
                prompt=KAZAKH_USER_PROMPT,
                instructions=KAZAKH_SYSTEM_INSTRUCTIONS,
                movie_name=movie_title,
                temperature=model_config["temperature"],
                rate_limit=model_config["rate_limit"],
                stream_responses=model_config["stream_responses"],
                postprocess_translation=True,
                break_long_lines=True,
                retry_on_error=False, # We handle retries through fallback
            )


            # Need to initialize the instruction explicitly since PySubtrans does logic on init_options
            py_options = init_options(
                provider=provider_name,
                model=model_config["model"],
                api_key=api_key,
                target_language="Kazakh",
                prompt=KAZAKH_USER_PROMPT,
                instructions=KAZAKH_SYSTEM_INSTRUCTIONS,
                movie_name=movie_title,
                # Keep the original global batching ones just to make init_options happy, 
                # but SubtitleTranslator won't re-batch.
                max_batch_size=BATCHING_CONFIG["max_batch_size"],
                scene_threshold=BATCHING_CONFIG["scene_threshold"],
                temperature=model_config["temperature"],
                rate_limit=model_config["rate_limit"],
                stream_responses=model_config["stream_responses"],
                postprocess_translation=True,
                break_long_lines=True,
                retry_on_error=False,
            )

            try:
                # Валидируем провайдер
                provider = init_translation_provider(provider_name, py_options)
                
                # ⛔ Напрямую, чтобы передать resume=True
                translator = SubtitleTranslator(py_options, provider, resume=True)
                
                logger.info(f"Starting/resuming translation with {model_config['model']}")
                translator.TranslateSubtitles(subtitles)
                
                # Если дошли сюда, весь перевод успешен
                subtitles.SaveTranslation(output_path)
                
                # Пытаемся подсчитать переведенные строки
                # У каждого батча есть поле translated.
                translated_lines_count = sum(len(b.translated) for b in subtitles.scenes if hasattr(b, 'translated') and b.translated)
                
                return {
                    "status": "success",
                    "output_path": output_path,
                    "lines_translated": translated_lines_count
                }
                
            except Exception as e:
                logger.warning(f"Translation failed with {model_config['model']}: {e}")
                last_exception = e
                # Fallback на следующую модель идёт автоматически

        # Исчерпали цепочку моделей. Проверяем частичный успех.
        translated_lines_count = 0
        if hasattr(subtitles, 'scenes'):
            translated_lines_count = sum(len(b.translated) for b in subtitles.scenes if hasattr(b, 'translated') and b.translated)

        if translated_lines_count > 0:
            logger.info("Exhausted all models. Partial translation found. Saving...")
            try:
                subtitles.SaveTranslation(output_path)
            except Exception as save_err:
                logger.error(f"Failed to save partial translation: {save_err}")
                
            # Обязанность Phase 1: выбрасывать Custom Exception
            raise PartialTranslationError(
                message=f"Translation partially failed after exhausting all models. Translated {translated_lines_count} lines.",
                output_path=output_path,
                lines_translated=translated_lines_count,
                original_error=last_exception
            )
        else:
            raise Exception("Translation failed completely with 0 lines translated.") from last_exception
