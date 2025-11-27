#!/bin/bash

# JungleBook Docker 컨테이너와 데이터 초기화 스크립트

set -e

echo "⚠️  ⚠️  ⚠️  경고: 이 스크립트는 모든 데이터를 삭제합니다!"
read -p "정말로 계속하시겠습니까? (yes를 입력하세요): " confirm

if [ "$confirm" != "yes" ]; then
    echo "❌ 취소되었습니다."
    exit 1
fi

# Docker Compose 파일이 있는 디렉토리로 이동
cd "$(dirname "$0")"

echo "🗑️  컨테이너 중지 및 삭제 중..."
docker-compose down -v

echo "🗑️  데이터 디렉토리 삭제 중..."
rm -rf datadir/mysql/*
rm -rf datadir/redis/*

echo "🚀 컨테이너 다시 시작 중..."
docker-compose up -d

echo "✅ 초기화 완료!"
echo ""
echo "MySQL: localhost:13306"
echo "Redis: localhost:16379"

