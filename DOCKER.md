# 🐳 Docker 사용 가이드

JungleBook 프로젝트의 Docker 사용법을 안내합니다.

## 📋 목차
- [빠른 시작](#빠른-시작)
- [Makefile 명령어](#makefile-명령어)
- [스크립트 사용법](#스크립트-사용법)
- [Docker Compose 직접 사용](#docker-compose-직접-사용)
- [고급 사용법](#고급-사용법)
- [문제 해결](#문제-해결)

## 🚀 빠른 시작

### 1. Docker 컨테이너 시작
```bash
make docker-up
```

### 2. 컨테이너 상태 확인
```bash
make docker-ps
```

### 3. 로그 확인
```bash
make docker-logs
```

## 📝 Makefile 명령어

프로젝트 루트에서 `make` 명령어를 사용할 수 있습니다.

### 기본 명령어

| 명령어 | 설명 |
|--------|------|
| `make help` | 모든 명령어 도움말 보기 |
| `make docker-up` | Docker 컨테이너 시작 (MySQL, Redis) |
| `make docker-down` | Docker 컨테이너 중지 |
| `make docker-restart` | Docker 컨테이너 재시작 |
| `make docker-ps` | 실행 중인 컨테이너 상태 확인 |

### 로그 관련

| 명령어 | 설명 |
|--------|------|
| `make docker-logs` | 모든 컨테이너 로그 확인 |
| `make docker-logs-mysql` | MySQL 컨테이너 로그만 확인 |
| `make docker-logs-redis` | Redis 컨테이너 로그만 확인 |

### 접속 및 실행

| 명령어 | 설명 |
|--------|------|
| `make docker-exec-mysql` | MySQL 컨테이너에 접속 |
| `make docker-exec-redis` | Redis 컨테이너에 접속 |
| `make docker-status` | 컨테이너 상태 및 리소스 사용량 확인 |
| `make docker-health` | 컨테이너 건강 상태 확인 |

### 관리 명령어

| 명령어 | 설명 |
|--------|------|
| `make docker-build` | Docker 이미지 빌드 (필요시) |
| `make docker-clean` | 컨테이너와 볼륨 삭제 (데이터 보존) |
| `make docker-reset` | 모든 데이터 삭제하고 초기화 ⚠️ |

## 🛠️ 스크립트 사용법

### docker-start.sh
```bash
./docker/docker-start.sh
```
컨테이너를 시작하고 상태를 확인합니다.

### docker-stop.sh
```bash
./docker/docker-stop.sh
```
컨테이너를 중지합니다.

### docker-reset.sh
```bash
./docker/docker-reset.sh
```
⚠️ **주의**: 모든 데이터를 삭제하고 초기화합니다.
실행 전에 확인 메시지가 표시됩니다.

## 🔧 Docker Compose 직접 사용

프로젝트 루트에서 직접 `docker-compose`를 사용할 수도 있습니다:

```bash
# docker 디렉토리로 이동
cd docker

# 컨테이너 시작
docker-compose up -d

# 컨테이너 중지
docker-compose down

# 로그 확인
docker-compose logs -f

# 특정 서비스 로그만 확인
docker-compose logs -f database
docker-compose logs -f redis

# 컨테이너 상태 확인
docker-compose ps

# 컨테이너 재시작
docker-compose restart

# 특정 서비스만 재시작
docker-compose restart database
docker-compose restart redis
```

## 💡 고급 사용법

### MySQL에 직접 접속
```bash
make docker-exec-mysql
```

또는:
```bash
cd docker
docker-compose exec database mysql -u junglebook -pjunglebook123!@# junglebook
```

### Redis에 직접 접속
```bash
make docker-exec-redis
```

또는:
```bash
cd docker
docker-compose exec redis redis-cli
```

### 컨테이너 내부에서 명령어 실행
```bash
# MySQL 컨테이너에서 명령어 실행
cd docker
docker-compose exec database ls -la

# Redis 컨테이너에서 명령어 실행
docker-compose exec redis redis-cli ping
```

### 볼륨 백업 및 복원
```bash
# 데이터 백업
docker-compose exec database mysqldump -u root -proot junglebook > backup.sql

# 데이터 복원
docker-compose exec -T database mysql -u root -proot junglebook < backup.sql
```

## 🔍 서비스 정보

### MySQL
- **포트**: 13306
- **컨테이너 이름**: junglebook-mysql
- **Database**: junglebook
- **Username**: junglebook
- **Password**: junglebook123!@#
- **Root Password**: root

### Redis
- **포트**: 16379
- **컨테이너 이름**: junglebook-redis
- **비밀번호**: 없음

## 🐛 문제 해결

### Docker 데몬이 실행되지 않을 때
```bash
# macOS
open -a Docker

# Linux
sudo systemctl start docker
```

### 포트가 이미 사용 중일 때
`docker/docker-compose.yml`에서 포트를 변경:
```yaml
ports:
  - "13307:3306"  # MySQL 포트 변경
  - "16380:6379"  # Redis 포트 변경
```

그리고 `application.yml`도 함께 수정해야 합니다.

### 컨테이너가 시작되지 않을 때
```bash
# 로그 확인
make docker-logs

# 컨테이너 상태 확인
make docker-ps

# 완전히 재시작
make docker-down
make docker-up
```

### 데이터베이스 연결 오류
1. 컨테이너가 실행 중인지 확인:
   ```bash
   make docker-ps
   ```

2. MySQL 로그 확인:
   ```bash
   make docker-logs-mysql
   ```

3. 데이터베이스에 직접 연결 테스트:
   ```bash
   make docker-exec-mysql
   ```

### 데이터 초기화가 필요할 때
```bash
# ⚠️ 주의: 모든 데이터가 삭제됩니다!
make docker-reset
```

## 📚 추가 리소스

- [Docker 공식 문서](https://docs.docker.com/)
- [Docker Compose 공식 문서](https://docs.docker.com/compose/)
- [MySQL Docker 이미지](https://hub.docker.com/_/mysql)
- [Redis Docker 이미지](https://hub.docker.com/_/redis)

