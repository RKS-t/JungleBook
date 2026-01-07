from typing import Optional
import logging
import sys
import os

# 프로젝트 루트를 경로에 추가
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

try:
    from config.settings import settings
except ImportError:
    # 설정 파일이 없을 경우 기본값 사용
    class Settings:
        AI_PROVIDER = os.getenv("AI_PROVIDER", "openai").lower()
        OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
        OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-4")
        GOOGLE_AI_API_KEY = os.getenv("GOOGLE_AI_API_KEY", "")
        GOOGLE_AI_MODEL = os.getenv("GOOGLE_AI_MODEL", "gemini-pro")
    settings = Settings()

logger = logging.getLogger(__name__)

class Translator:
    def __init__(self):
        self.provider = settings.AI_PROVIDER
        self.client = None
        self.enabled = False
        
        if self.provider == "google":
            if settings.GOOGLE_AI_API_KEY:
                try:
                    import google.generativeai as genai
                    genai.configure(api_key=settings.GOOGLE_AI_API_KEY)
                    self.client = genai
                    self.enabled = True
                    logger.info("Google AI API initialized successfully")
                except ImportError:
                    logger.error("google-generativeai package not installed. Please install it: pip install google-generativeai")
                except Exception as e:
                    logger.error(f"Failed to initialize Google AI API: {e}")
            else:
                logger.warning("Google AI API key not found. Translation disabled.")
        
        elif self.provider == "openai":
            if settings.OPENAI_API_KEY:
                try:
                    from openai import OpenAI
                    self.client = OpenAI(api_key=settings.OPENAI_API_KEY)
                    self.enabled = True
                    logger.info("OpenAI API initialized successfully")
                except ImportError:
                    logger.error("openai package not installed. Please install it: pip install openai")
                except Exception as e:
                    logger.error(f"Failed to initialize OpenAI API: {e}")
            else:
                logger.warning("OpenAI API key not found. Translation disabled.")
        
        else:
            logger.warning(f"Unknown AI provider: {self.provider}. Supported providers: 'openai', 'google'")
    
    def translate_to_english(self, text: str, source_language: str = "ko") -> Optional[str]:
        """텍스트를 영어로 번역"""
        if not self.enabled:
            logger.warning("Translation disabled. Returning original text.")
            return text
        
        try:
            if self.provider == "google":
                return self._translate_with_google(text, source_language, "en")
            elif self.provider == "openai":
                return self._translate_with_openai(text, source_language, "en")
        except Exception as e:
            logger.error(f"Translation failed: {e}")
            return None
    
    def translate_to_korean(self, text: str, source_language: str = "en") -> Optional[str]:
        """텍스트를 한국어로 번역 (초기 학습 데이터 준비용)"""
        if not self.enabled:
            logger.warning("Translation disabled. Returning original text.")
            return text
        
        try:
            if self.provider == "google":
                return self._translate_with_google(text, source_language, "ko")
            elif self.provider == "openai":
                return self._translate_with_openai(text, source_language, "ko")
        except Exception as e:
            logger.error(f"Translation to Korean failed: {e}")
            return None
    
    def _translate_with_openai(self, text: str, source_language: str, target_language: str) -> Optional[str]:
        """OpenAI API를 사용한 번역"""
        target_lang_name = "Korean" if target_language == "ko" else "English"
        source_lang_name = "Korean" if source_language == "ko" else "English"
        
        response = self.client.chat.completions.create(
            model=settings.OPENAI_MODEL,
            messages=[
                {
                    "role": "system",
                    "content": f"You are a professional translator. Translate the following {source_lang_name} text to {target_lang_name} accurately, preserving the meaning and context."
                },
                {
                    "role": "user",
                    "content": text
                }
            ],
            temperature=0.3,
            max_tokens=2000 if target_language == "ko" else 1000
        )
        
        translated_text = response.choices[0].message.content.strip()
        logger.info(f"OpenAI translation successful: {len(text)} -> {len(translated_text)} chars")
        return translated_text
    
    def _translate_with_google(self, text: str, source_language: str, target_language: str) -> Optional[str]:
        """Google AI API를 사용한 번역"""
        target_lang_name = "Korean" if target_language == "ko" else "English"
        source_lang_name = "Korean" if source_language == "ko" else "English"
        
        model = self.client.GenerativeModel(settings.GOOGLE_AI_MODEL)
        
        prompt = f"You are a professional translator. Translate the following {source_lang_name} text to {target_lang_name} accurately, preserving the meaning and context.\n\n{text}"
        
        response = model.generate_content(
            prompt,
            generation_config={
                "temperature": 0.3,
                "max_output_tokens": 2000 if target_language == "ko" else 1000,
            }
        )
        
        translated_text = response.text.strip()
        logger.info(f"Google AI translation successful: {len(text)} -> {len(translated_text)} chars")
        return translated_text
    
    def translate_batch(self, texts: list, source_language: str = "ko") -> list:
        """여러 텍스트를 일괄 번역"""
        if not self.enabled:
            return texts
        
        translated = []
        for text in texts:
            result = self.translate_to_english(text, source_language)
            translated.append(result if result else text)
        
        return translated
    
    def translate_batch_to_korean(self, texts: list, source_language: str = "en") -> list:
        """여러 텍스트를 일괄 한국어로 번역 (초기 학습 데이터 준비용)"""
        if not self.enabled:
            return texts
        
        translated = []
        for text in texts:
            result = self.translate_to_korean(text, source_language)
            translated.append(result if result else text)
        
        return translated
