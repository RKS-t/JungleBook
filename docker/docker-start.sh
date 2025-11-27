#!/bin/bash

# JungleBook Docker 컨테이너 시작 스크립트

set -e

echo "🚀 JungleBook Docker 컨테이너 시작 중..."

# Docker Compose 파일이 있는 디렉토리로 이동
cd "$(dirname "$0")"

# 컨테이너 시작
docker-compose up -d

# 컨테이너 상태 확인
echo ""
echo "📊 컨테이너 상태:"
docker-compose ps

echo ""
echo "✅ 컨테이너가 시작되었습니다!"
echo ""
echo "MySQL: localhost:13306"
echo "Redis: localhost:16379"
echo ""
echo "로그 확인: make docker-logs"
echo "중지: make docker-down"

