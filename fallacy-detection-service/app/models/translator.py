from openai import OpenAI
import logging
from typing import Optional
import sys
import os

# 프로젝트 루트를 경로에 추가
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

try:
    from config.settings import settings
except ImportError:
    # 설정 파일이 없을 경우 기본값 사용
    class Settings:
        OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
        OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-4")
    settings = Settings()

logger = logging.getLogger(__name__)

class Translator:
    def __init__(self):
        if settings.OPENAI_API_KEY:
            self.client = OpenAI(api_key=settings.OPENAI_API_KEY)
            self.enabled = True
        else:
            logger.warning("OpenAI API key not found. Translation disabled.")
            self.enabled = False
            self.client = None
    
    def translate_to_english(self, text: str, source_language: str = "ko") -> Optional[str]:
        """텍스트를 영어로 번역"""
        if not self.enabled:
            logger.warning("Translation disabled. Returning original text.")
            return text
        
        try:
            response = self.client.chat.completions.create(
                model=settings.OPENAI_MODEL,
                messages=[
                    {
                        "role": "system",
                        "content": f"You are a professional translator. Translate the following {source_language} text to English accurately, preserving the meaning and context."
                    },
                    {
                        "role": "user",
                        "content": text
                    }
                ],
                temperature=0.3,
                max_tokens=1000
            )
            
            translated_text = response.choices[0].message.content.strip()
            logger.info(f"Translation successful: {len(text)} -> {len(translated_text)} chars")
            return translated_text
        
        except Exception as e:
            logger.error(f"Translation failed: {e}")
            return None
    
    def translate_batch(self, texts: list, source_language: str = "ko") -> list:
        """여러 텍스트를 일괄 번역"""
        if not self.enabled:
            return texts
        
        translated = []
        for text in texts:
            result = self.translate_to_english(text, source_language)
            translated.append(result if result else text)
        
        return translated
    
    def translate_to_korean(self, text: str, source_language: str = "en") -> Optional[str]:
        """텍스트를 한국어로 번역 (초기 학습 데이터 준비용)"""
        if not self.enabled:
            logger.warning("Translation disabled. Returning original text.")
            return text
        
        try:
            response = self.client.chat.completions.create(
                model=settings.OPENAI_MODEL,
                messages=[
                    {
                        "role": "system",
                        "content": f"You are a professional translator. Translate the following {source_language} text to Korean accurately, preserving the meaning and context."
                    },
                    {
                        "role": "user",
                        "content": text
                    }
                ],
                temperature=0.3,
                max_tokens=2000
            )
            
            translated_text = response.choices[0].message.content.strip()
            logger.info(f"Translation to Korean successful: {len(text)} -> {len(translated_text)} chars")
            return translated_text
        
        except Exception as e:
            logger.error(f"Translation to Korean failed: {e}")
            return None
    
    def translate_batch_to_korean(self, texts: list, source_language: str = "en") -> list:
        """여러 텍스트를 일괄 한국어로 번역 (초기 학습 데이터 준비용)"""
        if not self.enabled:
            return texts
        
        translated = []
        for text in texts:
            result = self.translate_to_korean(text, source_language)
            translated.append(result if result else text)
        
        return translated

