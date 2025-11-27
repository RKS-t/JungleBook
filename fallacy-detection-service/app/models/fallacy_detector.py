import torch
from transformers import (
    AutoTokenizer, AutoModelForSequenceClassification
)
import json
import logging
import re
from typing import Dict, Optional, List
from dataclasses import dataclass, field

logger = logging.getLogger(__name__)

@dataclass
class FallacyResult:
    text: str
    has_fallacy: bool
    fallacy_type: Optional[str]
    confidence: float
    explanation: str
    topic_relevance: Optional[float] = None  # 주제 연관성 점수 (0.0 ~ 1.0)
    logical_structure: Optional[Dict] = field(default_factory=dict)  # 논리 구조 정보

class FallacyDetector:
    def __init__(self, model_path: str = None, model_name: str = "monologg/koelectra-base-v3-discriminator"):
        self.model_name = model_name
        self.model_path = model_path
        self.tokenizer = None
        self.model = None
        self.label_to_id = {}
        self.id_to_label = {}
        
        self.fallacy_definitions = {
            "ad_hominem": "인신공격: 논증의 내용이 아닌 논증자를 공격하는 오류",
            "straw_man": "허수아비 공격: 상대방의 논증을 왜곡하여 공격하는 오류",
            "false_dilemma": "허위 양자택일: 두 가지 선택지만 제시하는 오류",
            "appeal_to_emotion": "감정 호소: 논리적 근거 대신 감정을 이용하는 오류",
            "circular_reasoning": "순환 논증: 결론을 전제의 근거로 사용하는 오류",
            "hasty_generalization": "성급한 일반화: 제한된 사례에서 일반적 결론을 도출하는 오류",
            "false_cause": "허위 인과관계: 상관관계를 인과관계로 오해하는 오류",
            "bandwagon": "다수 찬성: 많은 사람이 믿는다는 이유로 옳다고 주장하는 오류",
            "appeal_to_authority": "권위에 호소: 부적절하거나 관련 없는 권위를 인용하는 오류",
            "red_herring": "빨간 청어: 주제에서 벗어난 정보로 주의를 분산시키는 오류",
            "equivocation": "애매한 표현: 단어나 구문을 여러 의미로 사용하는 오류",
            "fallacy_of_logic": "논리 오류: 논리적 추론 과정에서 발생하는 오류",
            "fallacy_of_credibility": "신뢰성 오류: 신뢰할 수 없는 출처를 인용하는 오류",
            "intentional": "의도적 오류: 의도적으로 논리적 오류를 범하는 경우",
            "no_fallacy": "논리적으로 타당한 논증"
        }
        
        # 모델이 없을 경우 기본 모드로 시작 (모델 로드 없이)
        try:
            self._load_model()
        except Exception as e:
            logger.warning(f"Model loading failed, using fallback mode: {e}")
            self.model = None
    
    def _load_model(self):
        """모델 로드"""
        try:
            if self.model_path:
                logger.info(f"Loading model from {self.model_path}")
                self.model = AutoModelForSequenceClassification.from_pretrained(self.model_path)
                self.tokenizer = AutoTokenizer.from_pretrained(self.model_path)
                
                # 라벨 매핑 로드
                try:
                    with open(f"{self.model_path}/label_mapping.json", "r", encoding="utf-8") as f:
                        mapping = json.load(f)
                        self.label_to_id = mapping.get("label_to_id", {})
                        self.id_to_label = {int(k): v for k, v in mapping.get("id_to_label", {}).items()}
                    logger.info("Label mapping loaded successfully")
                except Exception as e:
                    logger.warning(f"Could not load label mapping: {e}")
                    self._initialize_default_labels()
            else:
                logger.info(f"Using default model: {self.model_name}")
                self.tokenizer = AutoTokenizer.from_pretrained(self.model_name)
                self.model = AutoModelForSequenceClassification.from_pretrained(
                    self.model_name,
                    num_labels=11
                )
                self._initialize_default_labels()
            
            self.model.eval()
            logger.info("Model loaded successfully")
        except Exception as e:
            logger.error(f"Failed to load model: {e}")
            raise
    
    def _initialize_default_labels(self):
        """기본 라벨 초기화"""
        labels = [
            "ad_hominem", "straw_man", "false_dilemma", "appeal_to_emotion",
            "circular_reasoning", "hasty_generalization", "false_cause",
            "bandwagon", "appeal_to_authority", "red_herring", "no_fallacy"
        ]
        self.label_to_id = {label: idx for idx, label in enumerate(labels)}
        self.id_to_label = {idx: label for label, idx in self.label_to_id.items()}
    
    def predict(self, text: str, topic_title: Optional[str] = None, topic_description: Optional[str] = None) -> FallacyResult:
        """논리 오류 예측 (계층적 분석 및 청크 집계)"""
        if self.model is None or self.tokenizer is None:
            logger.warning("Model not loaded, returning default result")
            return FallacyResult(
                text=text,
                has_fallacy=False,
                fallacy_type=None,
                confidence=0.0,
                explanation="모델이 로드되지 않았습니다. 기본값을 반환합니다."
            )
        
        try:
            # 긴 텍스트 여부 확인
            max_length = 512
            tokenized = self.tokenizer(text, return_tensors="pt", truncation=False, padding=False)
            token_count = tokenized["input_ids"].shape[1]
            
            if token_count <= max_length:
                # 짧은 텍스트: 일반 분석
                return self._predict_single(text, topic_title, topic_description)
            else:
                # 긴 텍스트: 계층적 분석 및 청크 집계
                return self._predict_long_text(text, topic_title, topic_description, max_length)
                
        except Exception as e:
            logger.error(f"Prediction failed: {e}")
            return FallacyResult(
                text=text,
                has_fallacy=False,
                fallacy_type=None,
                confidence=0.0,
                explanation="분석 중 오류가 발생했습니다."
            )
    
    def _predict_single(self, text: str, topic_title: Optional[str] = None, topic_description: Optional[str] = None) -> FallacyResult:
        """단일 텍스트 분석"""
        inputs = self.tokenizer(
            text,
            return_tensors="pt",
            truncation=True,
            padding="max_length",
            max_length=512
        )
        
        with torch.no_grad():
            outputs = self.model(**inputs)
            predictions = torch.nn.functional.softmax(outputs.logits, dim=-1)
            predicted_class = torch.argmax(predictions, dim=-1).item()
            confidence = torch.max(predictions).item()
        
        predicted_label = self.id_to_label.get(predicted_class, "no_fallacy")
        has_fallacy = predicted_label != "no_fallacy"
        
        explanation = self.fallacy_definitions.get(
            predicted_label,
            "알 수 없는 오류 타입"
        )
        
        # 주제 연관성 계산
        topic_relevance = self._calculate_topic_relevance(text, topic_title, topic_description)
        
        # 논리 구조 분석
        logical_structure = self._analyze_logical_structure(text)
        
        return FallacyResult(
            text=text,
            has_fallacy=has_fallacy,
            fallacy_type=predicted_label if has_fallacy else None,
            confidence=float(confidence),
            explanation=explanation,
            topic_relevance=topic_relevance,
            logical_structure=logical_structure
        )
    
    def _predict_long_text(self, text: str, topic_title: Optional[str] = None, topic_description: Optional[str] = None, max_length: int = 512) -> FallacyResult:
        """긴 텍스트 계층적 분석 및 청크 집계"""
        # 1. 청크 분할 (슬라이딩 윈도우 방식)
        chunks = self._split_into_chunks(text, max_length)
        
        # 2. 각 청크 분석
        chunk_results = []
        for i, chunk in enumerate(chunks):
            try:
                result = self._predict_single(chunk, topic_title, topic_description)
                chunk_results.append({
                    'chunk_index': i,
                    'result': result,
                    'weight': self._calculate_chunk_weight(i, len(chunks), chunk)
                })
            except Exception as e:
                logger.warning(f"Chunk {i} analysis failed: {e}")
        
        if not chunk_results:
            return FallacyResult(
                text=text,
                has_fallacy=False,
                fallacy_type=None,
                confidence=0.0,
                explanation="청크 분석 실패"
            )
        
        # 3. 결과 집계 (가중 평균)
        aggregated_result = self._aggregate_chunk_results(chunk_results, text)
        
        # 4. 논리 구조 분석 (전체 텍스트)
        logical_structure = self._analyze_logical_structure(text)
        aggregated_result.logical_structure = logical_structure
        
        # 5. 주제 연관성 계산 (전체 텍스트)
        aggregated_result.topic_relevance = self._calculate_topic_relevance(text, topic_title, topic_description)
        
        return aggregated_result
    
    def _split_into_chunks(self, text: str, max_length: int, overlap: int = 100) -> List[str]:
        """텍스트를 청크로 분할 (슬라이딩 윈도우 방식)"""
        # 먼저 문장 단위로 분할
        sentences = self._split_into_sentences(text)
        
        if not sentences:
            return [text]
        
        chunks = []
        current_chunk = []
        current_length = 0
        
        for sentence in sentences:
            try:
                sentence_tokens = self.tokenizer(sentence, return_tensors="pt", truncation=False, padding=False)
                sentence_length = sentence_tokens["input_ids"].shape[1]
            except:
                # 토크나이징 실패 시 대략적인 길이 추정
                sentence_length = len(sentence) // 2  # 대략적인 토큰 수 추정
            
            if current_length + sentence_length > max_length - 50:  # 여유 공간
                if current_chunk:
                    chunks.append(" ".join(current_chunk))
                # 오버랩: 이전 청크의 마지막 2-3개 문장 포함
                overlap_count = min(3, len(current_chunk))
                overlap_sentences = current_chunk[-overlap_count:] if overlap_count > 0 else []
                current_chunk = overlap_sentences + [sentence]
                # 현재 청크 길이 재계산
                try:
                    current_length = sum([
                        len(self.tokenizer(s, return_tensors="pt", truncation=False, padding=False)["input_ids"][0]) 
                        for s in current_chunk
                    ])
                except:
                    current_length = sum([len(s) // 2 for s in current_chunk])
            else:
                current_chunk.append(sentence)
                current_length += sentence_length
        
        if current_chunk:
            chunks.append(" ".join(current_chunk))
        
        return chunks if chunks else [text]
    
    def _split_into_sentences(self, text: str) -> List[str]:
        """텍스트를 문장 단위로 분할 (한국어 최적화)"""
        # 한국어 문장 종결 기호로 분할
        # 마침표, 느낌표, 물음표, 한자 마침표 등
        sentences = re.split(r'[.!?。！？]\s*', text)
        # 빈 문장 제거 및 공백 정리
        sentences = [s.strip() for s in sentences if s.strip() and len(s.strip()) > 1]
        
        # 문장이 없으면 전체 텍스트를 하나의 문장으로 처리
        return sentences if sentences else [text]
    
    def _calculate_chunk_weight(self, chunk_index: int, total_chunks: int, chunk_text: str) -> float:
        """청크의 가중치 계산 (위치와 길이 고려)"""
        # 첫 부분과 끝 부분에 더 높은 가중치
        if chunk_index == 0:
            position_weight = 1.5  # 첫 부분
        elif chunk_index == total_chunks - 1:
            position_weight = 1.3  # 끝 부분
        else:
            position_weight = 1.0  # 중간 부분
        
        # 길이 가중치 (긴 청크에 더 높은 가중치)
        length_weight = min(len(chunk_text) / 200, 1.2)
        
        return position_weight * length_weight
    
    def _aggregate_chunk_results(self, chunk_results: List[Dict], full_text: str) -> FallacyResult:
        """청크 결과를 집계하여 최종 결과 생성"""
        total_weight = sum(r['weight'] for r in chunk_results)
        
        # 가중 평균 신뢰도 계산
        weighted_confidence = sum(
            r['result'].confidence * r['weight'] for r in chunk_results
        ) / total_weight if total_weight > 0 else 0.0
        
        # 가장 높은 신뢰도를 가진 오류 타입 선택
        fallacy_types = {}
        for r in chunk_results:
            if r['result'].has_fallacy and r['result'].fallacy_type:
                fallacy_type = r['result'].fallacy_type
                weighted_conf = r['result'].confidence * r['weight']
                if fallacy_type not in fallacy_types or fallacy_types[fallacy_type] < weighted_conf:
                    fallacy_types[fallacy_type] = weighted_conf
        
        # 최종 오류 타입 결정
        if fallacy_types:
            final_fallacy_type = max(fallacy_types.items(), key=lambda x: x[1])[0]
            has_fallacy = True
            final_confidence = fallacy_types[final_fallacy_type] / total_weight
            explanation = self.fallacy_definitions.get(final_fallacy_type, "알 수 없는 오류 타입")
            explanation += f" (긴 논증 분석: {len(chunk_results)}개 구간 분석 결과 집계)"
        else:
            final_fallacy_type = None
            has_fallacy = False
            final_confidence = 1.0 - weighted_confidence  # 오류 없음 신뢰도
            explanation = "논리적으로 타당한 논증 (긴 논증 전체 분석 결과)"
        
        return FallacyResult(
            text=full_text,
            has_fallacy=has_fallacy,
            fallacy_type=final_fallacy_type,
            confidence=float(final_confidence),
            explanation=explanation
        )
    
    def _calculate_topic_relevance(self, text: str, topic_title: Optional[str] = None, topic_description: Optional[str] = None) -> Optional[float]:
        """주제 연관성 점수 계산 (간단한 키워드 기반)"""
        if not topic_title and not topic_description:
            return None
        
        # 간단한 키워드 매칭 방식 (향후 개선 가능)
        topic_keywords = set()
        if topic_title:
            topic_keywords.update(topic_title.split())
        if topic_description:
            topic_keywords.update(topic_description.split())
        
        text_words = set(text.split())
        
        # 공통 키워드 비율 계산
        common_keywords = topic_keywords.intersection(text_words)
        if len(topic_keywords) > 0:
            relevance = len(common_keywords) / len(topic_keywords)
            return min(relevance, 1.0)
        
        return 0.5  # 기본값
    
    def _analyze_logical_structure(self, text: str) -> Dict:
        """논리 구조 분석 (문장 수, 단락 수, 논리 연결어 등)"""
        sentences = self._split_into_sentences(text)
        paragraphs = text.split('\n\n')
        
        # 논리 연결어 탐지
        logical_connectors = ['따라서', '그러므로', '그런데', '하지만', '그러나', '또한', '또', '그리고', '또한', '또한', '그러나', '하지만', '반면', '그러므로', '따라서', '결론적으로', '요약하면']
        connector_count = sum(1 for s in sentences for connector in logical_connectors if connector in s)
        
        return {
            'sentence_count': len(sentences),
            'paragraph_count': len([p for p in paragraphs if p.strip()]),
            'logical_connector_count': connector_count,
            'avg_sentence_length': sum(len(s) for s in sentences) / len(sentences) if sentences else 0,
            'text_length': len(text)
        }
    
    def _prepare_inputs(self, text: str, max_length: int = 512):
        """텍스트를 토크나이징 (이제는 _predict_long_text에서 처리)"""
        # 이 메서드는 이제 사용되지 않지만 호환성을 위해 유지
        return self.tokenizer(
            text,
            return_tensors="pt",
            truncation=True,
            padding="max_length",
            max_length=max_length
        )

