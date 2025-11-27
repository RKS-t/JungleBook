from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import logging
import sys
import os

# 프로젝트 루트를 경로에 추가
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

try:
    from config.settings import settings
except ImportError:
    # 설정 파일이 없을 경우 기본값 사용
    class Settings:
        API_TITLE = "Fallacy Detection Service"
        API_VERSION = "1.0.0"
        API_HOST = os.getenv("API_HOST", "0.0.0.0")
        API_PORT = int(os.getenv("API_PORT", "8000"))
    settings = Settings()

from app.api import routes

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)

app = FastAPI(
    title=settings.API_TITLE,
    version=settings.API_VERSION
)

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(routes.router, prefix="/api/v1", tags=["fallacy-detection"])

@app.get("/")
async def root():
    return {
        "service": "Fallacy Detection Service",
        "version": settings.API_VERSION,
        "status": "running"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.API_HOST,
        port=settings.API_PORT,
        reload=True
    )

