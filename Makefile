.PHONY: help docker-up docker-down docker-restart docker-logs docker-ps docker-clean docker-build docker-exec-mysql docker-exec-redis docker-logs-mysql docker-logs-redis docker-reset

# Docker Compose 파일 위치
DOCKER_COMPOSE = docker/docker-compose.yml
DOCKER_DIR = docker

help: ## 이 도움말 표시
	@echo "JungleBook 프로젝트 Docker 명령어"
	@echo ""
	@echo "사용법: make [target]"
	@echo ""
	@echo "주요 명령어:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

docker-up: ## Docker 컨테이너 시작 (MySQL, Redis)
	@echo "🚀 Docker 컨테이너 시작 중..."
	cd $(DOCKER_DIR) && docker-compose up -d
	@echo "✅ 컨테이너가 시작되었습니다!"
	@make docker-ps

docker-down: ## Docker 컨테이너 중지
	@echo "🛑 Docker 컨테이너 중지 중..."
	cd $(DOCKER_DIR) && docker-compose down
	@echo "✅ 컨테이너가 중지되었습니다!"

docker-restart: ## Docker 컨테이너 재시작
	@echo "🔄 Docker 컨테이너 재시작 중..."
	cd $(DOCKER_DIR) && docker-compose restart
	@echo "✅ 컨테이너가 재시작되었습니다!"

docker-logs: ## 모든 컨테이너 로그 확인
	cd $(DOCKER_DIR) && docker-compose logs -f

docker-logs-mysql: ## MySQL 컨테이너 로그 확인
	cd $(DOCKER_DIR) && docker-compose logs -f database

docker-logs-redis: ## Redis 컨테이너 로그 확인
	cd $(DOCKER_DIR) && docker-compose logs -f redis

docker-ps: ## 실행 중인 컨테이너 상태 확인
	@echo "📊 실행 중인 컨테이너:"
	cd $(DOCKER_DIR) && docker-compose ps

docker-clean: ## 컨테이너와 볼륨 삭제 (데이터 보존)
	@echo "⚠️  컨테이너와 볼륨을 삭제합니다 (데이터는 유지됩니다)"
	cd $(DOCKER_DIR) && docker-compose down -v
	@echo "✅ 정리 완료!"

docker-reset: ## 모든 데이터 삭제하고 초기화 (주의!)
	@echo "⚠️  ⚠️  ⚠️  경고: 모든 데이터가 삭제됩니다!"
	@read -p "정말로 계속하시겠습니까? (y/N): " confirm && [ "$$confirm" = "y" ] || exit 1
	@echo "🗑️  모든 컨테이너와 데이터 삭제 중..."
	cd $(DOCKER_DIR) && docker-compose down -v
	@echo "🗑️  데이터 디렉토리 삭제 중..."
	rm -rf $(DOCKER_DIR)/datadir/mysql/*
	rm -rf $(DOCKER_DIR)/datadir/redis/*
	@echo "🚀 컨테이너 다시 시작 중..."
	cd $(DOCKER_DIR) && docker-compose up -d
	@echo "✅ 초기화 완료!"

docker-exec-mysql: ## MySQL 컨테이너에 접속
	cd $(DOCKER_DIR) && docker-compose exec database mysql -u junglebook -pjunglebook123!@# junglebook

docker-exec-redis: ## Redis 컨테이너에 접속
	cd $(DOCKER_DIR) && docker-compose exec redis redis-cli

docker-build: ## Docker 이미지 빌드 (필요시)
	cd $(DOCKER_DIR) && docker-compose build

docker-status: ## 컨테이너 상태와 리소스 사용량 확인
	@echo "📊 컨테이너 상태:"
	cd $(DOCKER_DIR) && docker-compose ps
	@echo ""
	@echo "💾 리소스 사용량:"
	docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" junglebook-mysql junglebook-redis 2>/dev/null || echo "일부 컨테이너가 실행되지 않았습니다."

docker-health: ## 컨테이너 건강 상태 확인
	@echo "🏥 컨테이너 건강 상태:"
	@cd $(DOCKER_DIR) && docker-compose ps --format json | python3 -c "import json, sys; data = json.load(sys.stdin); [print(f\"{c['Name']}: {c['State']} ({c['Status']})\") for c in data]" 2>/dev/null || docker-compose ps

