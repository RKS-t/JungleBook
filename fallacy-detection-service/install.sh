#!/bin/bash
# Python 서비스 의존성 자동 설치 스크립트

set -e  # 오류 발생 시 스크립트 중단

echo "=== Python 서비스 의존성 설치 스크립트 ==="
echo ""

# 현재 디렉토리 확인
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Python 버전 확인
echo "1. Python 버전 확인 중..."
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3가 설치되어 있지 않습니다."
    echo "   Python 3.8 이상을 설치해주세요."
    exit 1
fi

PYTHON_VERSION=$(python3 --version 2>&1 | awk '{print $2}')
echo "✅ Python 버전: $PYTHON_VERSION"

# 가상 환경 생성
echo ""
echo "2. 가상 환경 생성 중..."
if [ ! -d "venv" ]; then
    python3 -m venv venv
    echo "✅ 가상 환경 생성 완료"
else
    echo "⚠️  가상 환경이 이미 존재합니다."
fi

# 가상 환경 활성화
echo ""
echo "3. 가상 환경 활성화 중..."
source venv/bin/activate
echo "✅ 가상 환경 활성화 완료"

# pip 업그레이드
echo ""
echo "4. pip 업그레이드 중..."
pip install --upgrade pip setuptools wheel --quiet
echo "✅ pip 업그레이드 완료"

# 의존성 설치
echo ""
echo "5. 의존성 설치 중... (시간이 오래 걸릴 수 있습니다)"
echo "   이 과정은 10-30분 정도 소요될 수 있습니다."
echo ""

if [ -f "requirements.txt" ]; then
    pip install -r requirements.txt
    echo ""
    echo "✅ 의존성 설치 완료"
else
    echo "❌ requirements.txt 파일을 찾을 수 없습니다."
    exit 1
fi

# .env 파일 확인
echo ""
echo "6. 환경 변수 파일 확인 중..."
if [ ! -f ".env" ]; then
    if [ -f ".env.example" ]; then
        cp .env.example .env
        echo "✅ .env 파일 생성 완료 (.env.example 기반)"
        echo "⚠️  .env 파일을 수정하여 OpenAI API 키를 설정하세요."
    else
        echo "⚠️  .env 파일이 없습니다. 필요시 수동으로 생성하세요."
    fi
else
    echo "✅ .env 파일이 이미 존재합니다."
fi

# 설치 확인
echo ""
echo "7. 설치 확인 중..."
python3 -c "
try:
    import fastapi
    import uvicorn
    import torch
    import transformers
    print('✅ 모든 주요 패키지 설치 확인 완료')
    print(f'   - PyTorch: {torch.__version__}')
    print(f'   - Transformers: {transformers.__version__}')
except ImportError as e:
    print(f'❌ 일부 패키지가 설치되지 않았습니다: {e}')
    exit(1)
"

if [ $? -eq 0 ]; then
    echo ""
    echo "=== 설치 완료 ==="
    echo ""
    echo "다음 단계:"
    echo "1. .env 파일을 수정하여 OpenAI API 키를 설정하세요 (선택)"
    echo "2. 서비스를 실행하세요:"
    echo "   source venv/bin/activate"
    echo "   python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload"
    echo ""
    echo "또는 스크립트를 사용하세요:"
    echo "   ./start_service.sh"
else
    echo ""
    echo "❌ 설치 확인 실패. 오류를 확인하세요."
    exit 1
fi

