#!/bin/bash

# JungleBook Docker 컨테이너 중지 스크립트

set -e

echo "🛑 JungleBook Docker 컨테이너 중지 중..."

# Docker Compose 파일이 있는 디렉토리로 이동
cd "$(dirname "$0")"

# 컨테이너 중지
docker-compose down

echo "✅ 컨테이너가 중지되었습니다!"

