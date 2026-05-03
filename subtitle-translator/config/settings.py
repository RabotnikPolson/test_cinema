from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # LLM
    GEMINI_API_KEY: str = ""
    OPENAI_API_KEY: str = ""
    OPENAI_WEBHOOK_SECRET: str = ""
    OPENAI_EXECUTION_MODE: str = "standard"  # or "batch"
    
    # Chunking
    GAP_THRESHOLD_SEC: float = 4.0
    MIN_LINES_PER_CHUNK: int = 10
    MAX_LINES_PER_CHUNK: int = 40
    
    # Storage
    STORAGE_BASE_PATH: str = "./storage"
    CHECKPOINT_DB_PATH: str = "./data/translator_state.db"
    
    # Java integration
    JAVA_WEBHOOK_URL: str = "http://localhost:8080/api/internal/subtitles/translation-complete"
    INTERNAL_SECRET: str = "change-me-in-prod"
    
    class Config:
        env_file = ".env"
