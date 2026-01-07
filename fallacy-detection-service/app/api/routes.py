from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional, List, Dict
import sys
import os

# 프로젝트 루트를 경로에 추가
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

from app.models.fallacy_detector import FallacyDetector, FallacyResult
from app.models.translator import Translator

try:
    from config.settings import settings
except ImportError:
    # 설정 파일이 없을 경우 기본값 사용
    class Settings:
        FALLACY_MODEL_PATH = os.getenv("FALLACY_MODEL_PATH", None)
        DEFAULT_MODEL_NAME = os.getenv("DEFAULT_MODEL_NAME", "distilbert-base-uncased")
        TRANSLATION_ENABLED = os.getenv("TRANSLATION_ENABLED", "true").lower() == "true"
    settings = Settings()

import logging

logger = logging.getLogger(__name__)

router = APIRouter()

# 전역 인스턴스
detector = None
translator = None

def initialize_services():
    """서비스 초기화"""
    global detector, translator
    
    try:
        # 모델 경로가 지정되어 있으면 로드 시도, 없으면 기본 모델명 사용
        model_path = settings.FALLACY_MODEL_PATH if hasattr(settings, 'FALLACY_MODEL_PATH') and settings.FALLACY_MODEL_PATH else None
        detector = FallacyDetector(
            model_path=model_path,
            model_name=settings.DEFAULT_MODEL_NAME
        )
        translator = Translator()
        logger.info("Services initialized successfully")
    except Exception as e:
        logger.warning(f"Failed to initialize services: {e}. Service will run in fallback mode.")
        # 폴백 모드로 계속 진행
        detector = None
        translator = Translator()

# 서비스 초기화
initialize_services()

# Request/Response 모델
class DetectRequest(BaseModel):
    text: str
    language: str = "ko"
    topic_title: Optional[str] = None
    topic_description: Optional[str] = None

class DetectResponse(BaseModel):
    has_fallacy: bool
    fallacy_type: Optional[str]
    confidence: float
    explanation: str
    topic_relevance: Optional[float] = None  # 주제 연관성 점수
    logical_structure: Optional[Dict] = None  # 논리 구조 정보

class BatchDetectRequest(BaseModel):
    texts: List[str]
    language: str = "ko"

class BatchDetectResponse(BaseModel):
    results: List[DetectResponse]

class RetrainRequest(BaseModel):
    training_data: List[Dict[str, str]]  # [{"text": "...", "label": "..."}]

class RetrainResponse(BaseModel):
    status: str
    model_path: str
    message: str

class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    translation_enabled: bool

@router.get("/health", response_model=HealthResponse)
async def health_check():
    """서비스 상태 확인"""
    return HealthResponse(
        status="healthy",
        model_loaded=detector is not None and detector.model is not None,
        translation_enabled=translator is not None and translator.enabled
    )

@router.post("/detect", response_model=DetectResponse)
async def detect_fallacy(request: DetectRequest):
    """단일 텍스트 논리 오류 탐지"""
    try:
        # 글자 수 제한 검증 (이중 체크)
        MAX_CONTENT_LENGTH = 5000
        if len(request.text) > MAX_CONTENT_LENGTH:
            raise HTTPException(
                status_code=400,
                detail=f"논증 내용은 최대 {MAX_CONTENT_LENGTH}자까지 작성할 수 있습니다. (현재: {len(request.text)}자)"
            )
        
        if detector is None:
            # 폴백 모드: 기본 응답 반환
            return DetectResponse(
                has_fallacy=False,
                fallacy_type=None,
                confidence=0.0,
                explanation="모델이 로드되지 않았습니다. 기본값을 반환합니다."
            )
        
        text = request.text
        
        # 논증의 부모 토픽 정보를 컨텍스트로 추가 (자연스러운 한국어 형식)
        # 토픽 정보가 있으면 항상 컨텍스트에 포함하여 분석 정확도 향상
        context_text = text
        if request.topic_title or request.topic_description:
            context_parts = []
            if request.topic_title:
                context_parts.append(f"토론 주제: {request.topic_title}")
            if request.topic_description:
                context_parts.append(f"주제 설명: {request.topic_description}")
            context_parts.append(f"다음은 위 주제에 대한 논증입니다: {text}")
            context_text = "\n\n".join(context_parts)
            logger.info("논증의 부모 토픽 정보가 컨텍스트에 포함되었습니다 (주제: %s)", request.topic_title or "제목 없음")
        else:
            # 토픽 정보가 없는 경우 (일반적으로 발생하지 않아야 함)
            logger.warning("논증의 부모 토픽 정보가 제공되지 않았습니다. 논증 내용만 분석합니다.")
        
        # 한국어 모델을 사용하므로 번역 불필요 (한국어 그대로 분석)
        # 영어 입력인 경우에만 한국어로 번역 (초기 학습 데이터 준비용)
        if request.language == "en" and settings.TRANSLATION_ENABLED and translator and translator.enabled:
            # 영어 입력은 한국어로 번역 후 분석 (선택적)
            translated = translator.translate_to_korean(context_text, "en")
            if translated:
                context_text = translated
                logger.info("영어 텍스트를 한국어로 번역하여 분석합니다")
            else:
                logger.warning("번역 실패: 원본 영어 텍스트를 사용합니다")
        
        # 논리 오류 탐지 (한국어 모델로 직접 분석, 계층적 분석 포함)
        result = detector.predict(
            context_text,
            topic_title=request.topic_title,
            topic_description=request.topic_description
        )
        
        return DetectResponse(
            has_fallacy=result.has_fallacy,
            fallacy_type=result.fallacy_type,
            confidence=result.confidence,
            explanation=result.explanation,
            topic_relevance=result.topic_relevance,
            logical_structure=result.logical_structure
        )
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Detection failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/detect/batch", response_model=BatchDetectResponse)
async def batch_detect_fallacy(request: BatchDetectRequest):
    """여러 텍스트 일괄 논리 오류 탐지"""
    try:
        if detector is None:
            # 폴백 모드: 기본 응답 반환
            results = [DetectResponse(
                has_fallacy=False,
                fallacy_type=None,
                confidence=0.0,
                explanation="모델이 로드되지 않았습니다. 기본값을 반환합니다."
            ) for _ in request.texts]
            return BatchDetectResponse(results=results)
        
        texts = request.texts
        
        # 한국어 모델을 사용하므로 번역 불필요 (한국어 그대로 분석)
        # 영어 입력인 경우에만 한국어로 번역
        if request.language == "en" and settings.TRANSLATION_ENABLED and translator and translator.enabled:
            translated_texts = translator.translate_batch_to_korean(texts, "en")
            texts = translated_texts
        
        # 일괄 탐지
        results = []
        for text in texts:
            result = detector.predict(text)
            results.append(DetectResponse(
                has_fallacy=result.has_fallacy,
                fallacy_type=result.fallacy_type,
                confidence=result.confidence,
                explanation=result.explanation
            ))
        
        return BatchDetectResponse(results=results)
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Batch detection failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/retrain", response_model=RetrainResponse)
async def retrain_model(request: RetrainRequest):
    """모델 재학습 (한국어 데이터 그대로 사용)"""
    try:
        from app.services.training_service import TrainingService
        
        # 재학습 데이터는 이미 한국어이므로 번역 불필요
        logger.info(f"한국어 샘플 {len(request.training_data)}개로 재학습을 시작합니다 (번역 불필요)")
        
        training_service = TrainingService()
        model_path = training_service.train_model(request.training_data)
        
        # 모델 재로드
        global detector
        detector = FallacyDetector(model_path=model_path)
        
        return RetrainResponse(
            status="success",
            model_path=model_path,
            message=f"Model retrained with {len(request.training_data)} Korean samples"
        )
    
    except Exception as e:
        logger.error(f"Retraining failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

