# 논리 오류 탐지 시스템 통합 가이드

## 개요
JungleBook 프로젝트에 논리 오류 탐지 시스템이 통합되었습니다. 사용자가 작성한 논증을 자동으로 분석하여 논리 오류를 탐지하고, 사용자가 의의를 제기할 수 있으며, 일정량 이상의 의의가 모이면 재학습을 진행합니다.

## 시스템 구조

### 1. Spring Boot (JungleBook)
- 논증 생성 시 자동으로 논리 오류 탐지
- 의의 제기 및 관리 시스템
- 재학습 데이터 수집 및 트리거

### 2. Python FastAPI 서비스
- 논리 오류 탐지 모델 실행 (한국어 KoELECTRA 기반)
- 영어 입력 시 선택적으로 한국어 번역 후 분석 (Google AI 또는 OpenAI)
- 모델 재학습

## 설치 및 실행

### Python 서비스 설정

1. **의존성 설치**
```bash
cd fallacy-detection-service
pip install -r requirements.txt
```

2. **환경 변수 설정**
```bash
cp .env.example .env
# .env 파일을 수정하여 AI API 키 및 모델 설정
# 예시 (Google AI 사용)
AI_PROVIDER=google
GOOGLE_AI_API_KEY=your_google_ai_api_key_here
GOOGLE_AI_MODEL=gemini-2.0-flash
TRANSLATION_ENABLED=true
# 예시 (OpenAI 사용)
# AI_PROVIDER=openai
# OPENAI_API_KEY=your_openai_api_key_here
# OPENAI_MODEL=gpt-4o-mini
```

3. **서비스 실행**
```bash
# 방법 1: 직접 실행
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# 방법 2: 스크립트 사용
./start_service.sh
```

### Spring Boot 설정

`application.yml`에 다음 설정이 추가되었습니다:
```yaml
fallacy:
  detection:
    service:
      url: http://localhost:8000/api/v1
      timeout: 5000
    appeal:
      threshold: 100
    translation:
      enabled: true  # 영어 입력 시 번역 후 분석
```

## API 엔드포인트

### 논증 관련
- `POST /api/debate/topics/{topicId}/arguments` - 논증 생성 (자동 논리 오류 탐지)
- `GET /api/debate/topics/{topicId}/arguments/{argumentId}` - 논증 상세 조회 (논리 오류 탐지 결과 포함)

### 논리 오류 관련
- `POST /api/debate/arguments/{argumentId}/fallacy/appeal` - 의의 제기
- `GET /api/debate/arguments/{argumentId}/fallacy/appeals` - 의의 목록 조회
- `GET /api/debate/arguments/{argumentId}/fallacy/appeals/count` - 의의 개수 조회
- `GET /api/debate/arguments/{argumentId}/fallacy/appeals/pending` - 대기 중인 의의 목록
- `POST /api/debate/arguments/{argumentId}/fallacy/appeals/{appealId}/approve` - 의의 승인 (관리자)
- `POST /api/debate/arguments/{argumentId}/fallacy/appeals/{appealId}/reject` - 의의 거부 (관리자)

### 재학습 관련
- `POST /api/debate/arguments/{argumentId}/fallacy/retrain` - 재학습 트리거 (관리자)
- `POST /api/debate/arguments/{argumentId}/fallacy/collect-training-data` - 재학습 데이터 수집 (관리자)
- `GET /api/debate/arguments/{argumentId}/fallacy/training-data/count` - 미사용 재학습 데이터 개수

## 데이터베이스 스키마

### 추가된 필드 (debate_argument 테이블)
- `fallacy_has_fallacy` - 논리 오류 여부
- `fallacy_type` - 논리 오류 타입
- `fallacy_confidence` - 신뢰도
- `fallacy_explanation` - 설명
- `fallacy_checked_yn` - 탐지 완료 여부

### 신규 테이블
- `debate_argument_fallacy_appeal` - 의의 제기 정보
- `debate_argument_fallacy_training_data` - 재학습용 데이터

## 작동 흐름

1. **논증 생성**
   - 사용자가 논증 작성
   - Spring Boot가 논증 저장
   - 비동기로 Python 서비스에 논리 오류 탐지 요청
   - 결과를 debate_argument 테이블에 저장

2. **의의 제기**
   - 사용자가 AI 판단에 의의 제기
   - 의의 정보 저장 (debate_argument_fallacy_appeal)
   - 재학습 임계값 확인 (기본 100개)

3. **재학습**
   - 임계값 도달 시 재학습 데이터 수집
   - Python 서비스에 재학습 요청
   - 모델 재학습 및 업데이트

## 논리 오류 타입

- `ad_hominem` - 인신공격
- `straw_man` - 허수아비 공격
- `false_dilemma` - 허위 양자택일
- `appeal_to_emotion` - 감정 호소
- `circular_reasoning` - 순환 논증
- `hasty_generalization` - 성급한 일반화
- `false_cause` - 허위 인과관계
- `bandwagon` - 다수 찬성
- `appeal_to_authority` - 권위에 호소
- `red_herring` - 빨간 청어
- `no_fallacy` - 논리적으로 타당한 논증

## 주의사항

1. **Python 서비스 의존성**
   - FastAPI, PyTorch, Transformers 등이 필요합니다
   - `pip install -r requirements.txt`로 설치하세요

2. **AI 번역 API 키**
   - 영어 입력을 번역하려면 Google AI 또는 OpenAI 키가 필요합니다
   - Google 권장 모델: `gemini-2.0-flash` (generateContent 지원)
   - `.env` 파일에 `AI_PROVIDER`, `GOOGLE_AI_API_KEY` 또는 `OPENAI_API_KEY`를 설정하세요

3. **모델 초기 학습**
   - 처음 실행 시 모델이 없으면 폴백 모드로 동작합니다
   - 실제 모델 학습은 별도로 진행해야 합니다

4. **재학습 임계값**
   - 기본값은 100개입니다
   - `application.yml`에서 조정 가능합니다

## 다음 단계

1. Python 서비스 의존성 설치 및 모델 초기 학습
2. 영어 입력 시 번역 활성화 후 실제 논리 오류 탐지 테스트 (한국어 입력은 직접 분석)
3. 재학습 파이프라인 완성도 향상
4. 영어 학습 데이터 추가 학습 또는 영어 전용 모델 검토 (현재 영어 입력 신뢰도 낮음)

