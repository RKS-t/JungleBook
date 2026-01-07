import os
from dotenv import load_dotenv

load_dotenv()

class Settings:
    # FastAPI 설정
    API_TITLE: str = "Fallacy Detection Service"
    API_VERSION: str = "1.0.0"
    API_HOST: str = os.getenv("API_HOST", "0.0.0.0")
    API_PORT: int = int(os.getenv("API_PORT", "8000"))
    
    # AI API 설정 (OpenAI 또는 Google AI 선택)
    AI_PROVIDER: str = os.getenv("AI_PROVIDER", "openai").lower()  # "openai" 또는 "google"
    
    # OpenAI API 설정
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")
    OPENAI_MODEL: str = os.getenv("OPENAI_MODEL", "gpt-4")
    
    # Google AI API 설정
    GOOGLE_AI_API_KEY: str = os.getenv("GOOGLE_AI_API_KEY", "")
    GOOGLE_AI_MODEL: str = os.getenv("GOOGLE_AI_MODEL", "gemini-2.0-flash")
    
    # 모델 설정
    FALLACY_MODEL_PATH: str = os.getenv("FALLACY_MODEL_PATH", "./models/fallacy_detector")
    DEFAULT_MODEL_NAME: str = os.getenv("DEFAULT_MODEL_NAME", "monologg/koelectra-base-v3-discriminator")  # 한국어 모델
    
    # 재학습 설정
    RETRAIN_THRESHOLD: int = int(os.getenv("RETRAIN_THRESHOLD", "100"))
    TRAINING_DATA_PATH: str = os.getenv("TRAINING_DATA_PATH", "./data/training")
    
    # 번역 설정
    TRANSLATION_ENABLED: bool = os.getenv("TRANSLATION_ENABLED", "true").lower() == "true"
    TARGET_LANGUAGE: str = os.getenv("TARGET_LANGUAGE", "ko")

settings = Settings()

