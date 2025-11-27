#!/bin/bash
# Python 서비스 실행 스크립트

cd "$(dirname "$0")"

echo "=== Fallacy Detection Service 시작 ==="
echo ""

# 가상 환경 확인 (선택사항)
if [ -d "venv" ]; then
    echo "가상 환경 활성화..."
    source venv/bin/activate
fi

# 의존성 설치 확인
if ! python3 -c "import fastapi" 2>/dev/null; then
    echo "⚠️ FastAPI가 설치되지 않았습니다."
    echo "다음 명령어로 설치하세요: pip install -r requirements.txt"
    echo ""
    echo "계속 진행하시겠습니까? (y/N)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 서비스 실행
echo "서비스 시작 중..."
python3 -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

