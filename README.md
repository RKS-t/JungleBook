# JungleBook 프로젝트

JungleBook 프로젝트 README

## 🚀 빠른 시작

### 필수 요구사항
- Java 17 이상
- Gradle 8.x 이상
- Docker & Docker Compose
- MySQL 8.0
- Redis 7.0

## 🐳 Docker 사용법

프로젝트에는 Docker를 사용하여 개발 환경을 쉽게 설정할 수 있는 설정이 포함되어 있습니다.

### Makefile을 사용한 Docker 명령어 (추천)

프로젝트 루트에서 다음 명령어를 사용할 수 있습니다:

```bash
# 도움말 보기
make help

# Docker 컨테이너 시작 (MySQL, Redis)
make docker-up

# Docker 컨테이너 중지
make docker-down

# Docker 컨테이너 재시작
make docker-restart

# 실행 중인 컨테이너 상태 확인
make docker-ps

# 모든 컨테이너 로그 확인
make docker-logs

# MySQL 로그만 확인
make docker-logs-mysql

# Redis 로그만 확인
make docker-logs-redis

# MySQL 컨테이너에 접속
make docker-exec-mysql

# Redis 컨테이너에 접속
make docker-exec-redis

# 컨테이너 상태 및 리소스 사용량 확인
make docker-status

# 모든 데이터 초기화 (주의!)
make docker-reset
```

### 스크립트를 사용한 Docker 명령어

```bash
# 컨테이너 시작
./docker/docker-start.sh

# 컨테이너 중지
./docker/docker-stop.sh

# 모든 데이터 초기화 (주의!)
./docker/docker-reset.sh
```

### Docker Compose 직접 사용

```bash
# docker 디렉토리로 이동
cd docker

# 컨테이너 시작
docker-compose up -d

# 컨테이너 중지
docker-compose down

# 로그 확인
docker-compose logs -f

# 컨테이너 상태 확인
docker-compose ps
```

### Docker 서비스 정보

| 서비스 | 포트 | 컨테이너 이름 | 설명 |
|--------|------|--------------|------|
| MySQL | 13306 | junglebook-mysql | 데이터베이스 |
| Redis | 16379 | junglebook-redis | 캐시/세션 저장소 |

**연결 정보:**
- MySQL: `localhost:13306`
- Redis: `localhost:16379`

**MySQL 접속 정보:**
- Database: `junglebook`
- Username: `junglebook`
- Password: `junglebook123!@#`
- Root Password: `root`

## 📦 프로젝트 설정

### 데이터베이스 설정

`application.yml`에서 다음과 같이 설정되어 있습니다:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:13306/junglebook
    username: junglebook
    password: junglebook123!@#
```

### 빌드 및 실행

```bash
# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew test
```

## 🛠️ 개발 환경 설정

1. Docker 컨테이너 시작:
   ```bash
   make docker-up
   ```

2. 데이터베이스 스키마 초기화:
   - 컨테이너 시작 시 자동으로 `docker/schema/init.sql`이 실행됩니다.

3. 애플리케이션 실행:
   ```bash
   ./gradlew bootRun
   ```

## 📝 주요 명령어 요약

### Docker
- `make docker-up` - 컨테이너 시작
- `make docker-down` - 컨테이너 중지
- `make docker-restart` - 컨테이너 재시작
- `make docker-logs` - 로그 확인
- `make docker-ps` - 상태 확인

### Gradle
- `./gradlew build` - 빌드
- `./gradlew bootRun` - 실행
- `./gradlew test` - 테스트
- `./gradlew clean` - 정리

## 🐛 문제 해결

### 포트 충돌
만약 13306 또는 16379 포트가 이미 사용 중이라면, `docker/docker-compose.yml`에서 포트를 변경할 수 있습니다.

### 데이터 초기화
모든 데이터를 삭제하고 처음부터 시작하려면:
```bash
make docker-reset
```

주의: 이 명령어는 모든 데이터를 삭제합니다!

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

## 📚 참고 자료

- [Docker 공식 문서](https://docs.docker.com/)
- [Docker Compose 공식 문서](https://docs.docker.com/compose/)
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)