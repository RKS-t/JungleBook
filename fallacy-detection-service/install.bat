@echo off
REM Python 서비스 의존성 자동 설치 스크립트 (Windows)

echo === Python 서비스 의존성 설치 스크립트 ===
echo.

REM Python 버전 확인
echo 1. Python 버전 확인 중...
python --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Python이 설치되어 있지 않습니다.
    echo    Python 3.8 이상을 설치해주세요.
    pause
    exit /b 1
)

python --version
echo ✅ Python 확인 완료

REM 가상 환경 생성
echo.
echo 2. 가상 환경 생성 중...
if not exist "venv" (
    python -m venv venv
    echo ✅ 가상 환경 생성 완료
) else (
    echo ⚠️  가상 환경이 이미 존재합니다.
)

REM 가상 환경 활성화
echo.
echo 3. 가상 환경 활성화 중...
call venv\Scripts\activate.bat
if errorlevel 1 (
    echo ❌ 가상 환경 활성화 실패
    pause
    exit /b 1
)
echo ✅ 가상 환경 활성화 완료

REM pip 업그레이드
echo.
echo 4. pip 업그레이드 중...
python -m pip install --upgrade pip setuptools wheel --quiet
echo ✅ pip 업그레이드 완료

REM 의존성 설치
echo.
echo 5. 의존성 설치 중... (시간이 오래 걸릴 수 있습니다)
echo    이 과정은 10-30분 정도 소요될 수 있습니다.
echo.

if exist "requirements.txt" (
    pip install -r requirements.txt
    echo.
    echo ✅ 의존성 설치 완료
) else (
    echo ❌ requirements.txt 파일을 찾을 수 없습니다.
    pause
    exit /b 1
)

REM .env 파일 확인
echo.
echo 6. 환경 변수 파일 확인 중...
if not exist ".env" (
    if exist ".env.example" (
        copy .env.example .env
        echo ✅ .env 파일 생성 완료 (.env.example 기반)
        echo ⚠️  .env 파일을 수정하여 OpenAI API 키를 설정하세요.
    ) else (
        echo ⚠️  .env 파일이 없습니다. 필요시 수동으로 생성하세요.
    )
) else (
    echo ✅ .env 파일이 이미 존재합니다.
)

REM 설치 확인
echo.
echo 7. 설치 확인 중...
python -c "import fastapi; import uvicorn; import torch; import transformers; print('✅ 모든 주요 패키지 설치 확인 완료'); print(f'   - PyTorch: {torch.__version__}'); print(f'   - Transformers: {transformers.__version__}')"

if errorlevel 1 (
    echo.
    echo ❌ 설치 확인 실패. 오류를 확인하세요.
    pause
    exit /b 1
)

echo.
echo === 설치 완료 ===
echo.
echo 다음 단계:
echo 1. .env 파일을 수정하여 OpenAI API 키를 설정하세요 (선택)
echo 2. 서비스를 실행하세요:
echo    venv\Scripts\activate
echo    python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
echo.
pause

