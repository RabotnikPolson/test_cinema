import logging
import os
from dotenv import load_dotenv

from PySubtrans import init_options, init_subtitles
from config.models import BATCHING_CONFIG
from config.kazakh_prompt import KAZAKH_USER_PROMPT, KAZAKH_SYSTEM_INSTRUCTIONS
from services.fallback_orchestrator import FallbackOrchestrator

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

class TranslationService:
    def __init__(self):
        self.orchestrator = FallbackOrchestrator()

    def translate(self, input_path: str, output_path: str, movie_title: str, source_language: str = "ru"):
        """
        Main entrypoint for subtitle translation via PySubtrans.
        Synchronous, blocking call.
        """
        logger.info(f"Preparing to translate {input_path} -> {output_path} for '{movie_title}'")
        
        # 1. Сначала подгружаем глобальные настройки для батчинга (один раз!)
        # PySubtrans инициализирует батчинг на основе options, переданных в init_subtitles
        options = init_options(
            provider="Gemini", # Placeholder, actually not used for batching but required by API 
            model="gemini-3.1-flash-lite-preview",
            api_key="TEST", # Required by validation but we overwrite in orchestrator
            target_language="Kazakh",
            prompt=KAZAKH_USER_PROMPT,
            instructions=KAZAKH_SYSTEM_INSTRUCTIONS,
            movie_name=movie_title,
            max_batch_size=BATCHING_CONFIG["max_batch_size"],
            scene_threshold=BATCHING_CONFIG["scene_threshold"],
        )

        # 2. Инициализируем субтитры (разбивка на батчи происходит ЗДЕСЬ)
        try:
            subtitles = init_subtitles(input_path, options=options)
            num_scenes = len(subtitles.scenes) if hasattr(subtitles, 'scenes') else 0
            logger.info(f"Subtitles loaded and batched into {num_scenes} scenes.")
        except Exception as e:
            logger.error(f"Failed to load/batch subtitles: {e}")
            raise e

        # 3. Передаем в FallbackOrchestrator
        return self.orchestrator.execute_with_fallback(subtitles, movie_title, output_path)

if __name__ == "__main__":
    # Загружаем .env для локального тестирования
    load_dotenv()
    
    # Создаем директорию для теста если не существует
    os.makedirs("test_data", exist_ok=True)
    
    # Входной и выходной файлы (теперь в .srt формате)
    input_file = "test_data/sample.srt"
    output_file = "test_data/sample_kk.srt"
    
    # Создаем тестовый .srt если его нет
    if not os.path.exists(input_file):
        with open(input_file, "w", encoding="utf-8") as f:
            f.write("1\n00:00:01,000 --> 00:00:04,000\nПривет. Как дела?\n\n2\n00:00:05,000 --> 00:00:08,000\nЯ думаю, нам нужно поговорить обо всем этом.\n")
        logger.info(f"Created sample SRT file at {input_file}")

    # Запускаем переводчик
    service = TranslationService()
    try:
        result = service.translate(
            input_path=input_file,
            output_path=output_file,
            movie_title="Тестовый фильм"
        )
        logger.info(f"Translation completed! Result: {result}")
    except Exception as e:
        logger.error(f"Translation strictly failed or partial: {e}")

