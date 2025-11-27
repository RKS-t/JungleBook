# 논리 오류 탐지 서비스 (Fallacy Detection Service)

JungleBook 프로젝트의 논리 오류 탐지 시스템을 위한 Python FastAPI 서비스입니다.

## 개요

이 서비스는 사용자가 작성한 논증을 분석하여 논리 오류를 탐지합니다. **한국어 모델(KoELECTRA)**을 사용하여 한국어 텍스트를 직접 분석합니다.

## 주요 기능

- **논리 오류 탐지**: 15가지 논리 오류 타입 탐지 (한국어 모델)
- **한국어 직접 분석**: 한국어 텍스트를 번역 없이 직접 분석 (번역 단계 제거)
- **Topic 컨텍스트 분석**: 토론 주제 정보를 포함한 맥락 분석
- **긴 논증 처리**: 512 토큰까지 지원, 긴 텍스트는 청킹 처리
- **모델 재학습**: 한국어 재학습 데이터를 그대로 사용하여 정확도 향상
- **REST API**: FastAPI 기반 RESTful API 제공

## 설치

### 1. 의존성 설치

```bash
# 가상 환경 생성
python3 -m venv venv
source venv/bin/activate  # macOS/Linux
# 또는
venv\Scripts\activate  # Windows

# 의존성 설치
pip install -r requirements.txt
```

자동 설치 스크립트 사용:
```bash
./install.sh  # macOS/Linux
install.bat   # Windows
```

### 2. 환경 변수 설정

`.env` 파일 생성:
```bash
cp .env.example .env
```

`.env` 파일 수정:
```env
# OpenAI API 설정 (번역 기능용, 선택사항)
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_MODEL=gpt-4
TRANSLATION_ENABLED=false

# 모델 설정
FALLACY_MODEL_PATH=./models/huggingface_real_training
DEFAULT_MODEL_NAME=monologg/koelectra-base-v3-discriminator  # 한국어 모델

# 서비스 설정
API_HOST=0.0.0.0
API_PORT=8000
```

## 실행

### 직접 실행

```bash
source venv/bin/activate
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 스크립트 사용

```bash
./start_service.sh
```

## API 엔드포인트

### Health Check
```
GET /api/v1/health
```

### 논리 오류 탐지
```
POST /api/v1/detect
Content-Type: application/json

{
  "text": "분석할 텍스트",
  "language": "ko",
  "topic_title": "토론 주제 (선택사항)",
  "topic_description": "주제 설명 (선택사항)"
}
```

### 일괄 탐지
```
POST /api/v1/detect/batch
Content-Type: application/json

{
  "texts": ["텍스트1", "텍스트2"],
  "language": "ko"
}
```

### 모델 재학습
```
POST /api/v1/retrain
Content-Type: application/json

{
  "training_data": [
    {"text": "한국어 학습 데이터", "label": "fallacy_type"}
  ]
}
```
**참고**: 재학습 데이터는 한국어 그대로 사용됩니다 (번역 불필요)

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
- `equivocation` - 애매한 표현
- `fallacy_of_logic` - 논리 오류
- `fallacy_of_credibility` - 신뢰성 오류
- `intentional` - 의도적 오류
- `no_fallacy` - 논리적으로 타당한 논증

## 프로젝트 구조

```
fallacy-detection-service/
├── app/
│   ├── main.py              # FastAPI 애플리케이션
│   ├── api/
│   │   └── routes.py        # API 라우트
│   ├── models/
│   │   ├── fallacy_detector.py  # 논리 오류 탐지 모델
│   │   └── translator.py       # 번역 서비스
│   └── services/
│       └── training_service.py  # 모델 재학습 서비스
├── config/
│   └── settings.py          # 설정 파일
├── models/
│   └── huggingface_real_training/  # 학습된 모델
├── requirements.txt         # Python 의존성
├── .env                     # 환경 변수 (생성 필요)
├── .env.example            # 환경 변수 템플릿
├── .gitignore              # Git 무시 파일
├── install.sh              # 설치 스크립트 (macOS/Linux)
├── install.bat             # 설치 스크립트 (Windows)
├── start_service.sh        # 서비스 실행 스크립트
└── README.md               # 이 파일
```

## 개발

### 모델 학습

초기 모델은 HuggingFace의 `tasksource/logical-fallacy` 데이터셋을 한국어로 번역하여 학습되었습니다.

재학습을 위해서는:
1. Spring Boot에서 의의 제기 데이터 수집 (한국어)
2. 100개 이상의 데이터가 모이면 자동 재학습 트리거
3. 재학습 데이터는 한국어 그대로 사용 (번역 불필요, 정확도 향상)
4. 또는 `/api/v1/retrain` API를 직접 호출

**초기 학습 데이터 준비**:
```bash
python scripts/prepare_korean_training_data.py
```
영어 데이터를 한국어로 번역하여 초기 학습 데이터를 준비합니다.

## 참고 자료

- [HuggingFace 논리 오류 데이터셋](https://huggingface.co/datasets/tasksource/logical-fallacy)
- [논문: Logical Fallacy Detection](https://arxiv.org/abs/2202.13758)

## 라이선스

JungleBook 프로젝트의 일부입니다.
