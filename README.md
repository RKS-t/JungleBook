# JungleBook 프로젝트

## 📖 프로젝트 소개

JungleBook은 정치·사회 이슈에 대한 건전한 토론과 의견 교환을 지원하는 플랫폼입니다. 사용자들이 다양한 주제에 대해 논증을 제시하고, AI 기반 논리 오류 탐지 시스템을 통해 논리적 사고를 향상시킬 수 있습니다.

### 주요 기능

#### 1. 토론 시스템 (Debate)
- **토픽 생성 및 관리**: 다양한 카테고리별 토론 토픽 생성
- **논증 작성**: 찬성/반대 입장별 논증 작성 및 관리
- **댓글 시스템**: 논증에 대한 댓글 및 대댓글 작성
- **투표 기능**: 논증에 대한 지지/반대 투표
- **통계 및 분석**: 토픽별 통계, 입장별 분포 등

#### 2. 게시판 시스템 (Post)
- **게시글 작성**: 다양한 게시판에 게시글 작성
- **댓글 및 답글**: 게시글에 대한 댓글 및 대댓글
- **좋아요 기능**: 게시글 및 댓글 좋아요
- **파일 업로드**: 게시글 첨부 파일 업로드
- **검색 및 정렬**: 제목, 내용, 작성자별 검색 및 정렬

#### 3. 회원 관리 (Member)
- **일반 회원가입/로그인**: 이메일 기반 회원가입 및 로그인
- **소셜 로그인**: 카카오, 네이버 소셜 로그인 지원
- **JWT 인증**: Access Token 및 Refresh Token 기반 인증
- **프로필 관리**: 회원 정보 조회 및 수정

#### 4. 논리 오류 탐지 시스템 (Fallacy Detection)
- **자동 탐지**: 논증 작성 시 자동으로 논리 오류 탐지
- **다양한 오류 타입**: 13가지 논리 오류 타입 탐지
- **의의 제기**: 사용자가 AI 판단에 대해 의의 제기 가능
- **모델 재학습**: 의의 데이터 축적 시 모델 자동 재학습
- **한국어 모델**: KoELECTRA 기반 한국어 전용 모델 사용

#### 5. 국회 정보 연동 (Assembly)
- **법안 조회**: 국회 법안 정보 조회
- **찬반 투표 정보**: 법안별 찬반 투표 현황
- **정당별 분석**: 정당별 찬반 투표 통계

## 🏗️ 기술 스택

### Backend
- **언어**: Kotlin
- **프레임워크**: Spring Boot 3.x
- **인증**: Spring Security + JWT
- **데이터베이스**: MySQL 8.0
- **캐시**: Redis 7.0
- **빌드 도구**: Gradle

### AI/ML 서비스
- **언어**: Python 3.x
- **프레임워크**: FastAPI
- **모델**: KoELECTRA (한국어 논리 오류 탐지)
- **번역**: Google AI / OpenAI API

### 인프라
- **컨테이너화**: Docker & Docker Compose
- **데이터베이스**: MySQL 8.0
- **캐시**: Redis 7.0

## 📁 프로젝트 구조

```
JungleBook/
├── src/main/kotlin/org/example/junglebook/
│   ├── web/controller/          # REST API 컨트롤러
│   │   ├── debate/              # 토론 관련 API
│   │   ├── post/                # 게시판 관련 API
│   │   └── MemberController.kt  # 회원 관련 API
│   ├── service/                 # 비즈니스 로직
│   │   ├── debate/              # 토론 서비스
│   │   ├── post/                # 게시판 서비스
│   │   └── MemberService.kt     # 회원 서비스
│   ├── repository/              # 데이터 접근 계층
│   ├── entity/                  # JPA 엔티티
│   ├── config/                  # 설정 클래스
│   │   ├── SecurityConfig.kt   # Spring Security 설정
│   │   └── RedisConfig.kt      # Redis 설정
│   └── filter/                  # 필터
│       └── JwtAuthenticationFilter.kt
├── fallacy-detection-service/   # Python AI 서비스
│   ├── app/                     # FastAPI 애플리케이션
│   ├── models/                  # 학습된 모델
│   └── scripts/                 # 학습 스크립트
├── docker/                      # Docker 설정
│   ├── docker-compose.yml      # Docker Compose 설정
│   └── schema/                  # 데이터베이스 스키마
├── doc/                         # API 다이어그램 문서
└── scripts/                     # 유틸리티 스크립트
```

## 🚀 빠른 시작

### 필수 요구사항
- Java 17 이상
- Gradle 8.x 이상
- Docker & Docker Compose
- MySQL 8.0
- Redis 7.0
- Python 3.x (논리 오류 탐지 서비스용)

### 1. Docker 컨테이너 시작

데이터베이스와 Redis를 시작합니다:

```bash
make docker-up
```

자세한 Docker 사용법은 [DOCKER.md](DOCKER.md)를 참고하세요.

### 2. 애플리케이션 실행

```bash
# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun
```

### 3. 논리 오류 탐지 서비스 실행 (선택사항)

```bash
cd fallacy-detection-service
source venv/bin/activate
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

자세한 설정은 [FALLACY_DETECTION_README.md](FALLACY_DETECTION_README.md)를 참고하세요.

## 📊 API 다이어그램

프로젝트의 주요 API에 대한 상세한 다이어그램이 `doc/` 디렉토리에 정리되어 있습니다.

### 다이어그램 종류
각 API 엔드포인트마다 다음 5가지 다이어그램이 제공됩니다:
- **Class Diagram**: 클래스 구조와 관계
- **Sequence Diagram**: 컴포넌트 간 상호작용 흐름 (JWT 필터 체인 포함)
- **Flowchart**: 비즈니스 로직 흐름 (GlobalExceptionHandler 포함)
- **Redis Type Map**: Redis 키 구조 (미사용 시 "No Redis usage" 표기)
- **Database ERD**: 데이터베이스 스키마와 관계

### 바로가기
- **전체 목차**: [doc/README.md](doc/README.md)
- **주요 API 예시**:
  - [게시글 생성 Flowchart](doc/post/PostController/createPost-flowchart.md)
  - [회원 가입+로그인 Flowchart](doc/MemberController/signupAndLogin-flowchart.md)
  - [토론 토픽 생성 Flowchart](doc/debate/DebateTopicController/createTopic-flowchart.md)
  - [토론 논증 생성 Flowchart](doc/debate/DebateArgumentController/createArgument-flowchart.md)

## 🔐 보안

### 인증 및 인가
- JWT 기반 인증 (Access Token + Refresh Token)
- Spring Security를 통한 엔드포인트 보호
- 공개 엔드포인트: 로그인, 회원가입, 토픽 조회, 게시판 조회
- 인증 필요 엔드포인트: 게시글/논증 작성, 수정, 삭제

### 권한 관리
- 작성자만 수정/삭제 가능
- 논증 작성자만 의의 제기 가능
- GlobalExceptionHandler를 통한 일관된 에러 처리

## 📝 주요 명령어

### Docker
```bash
make docker-up      # 컨테이너 시작
make docker-down    # 컨테이너 중지
make docker-restart # 컨테이너 재시작
make docker-logs   # 로그 확인
make docker-ps      # 상태 확인
```

자세한 Docker 명령어는 [DOCKER.md](DOCKER.md)를 참고하세요.

### Gradle
```bash
./gradlew build     # 빌드
./gradlew bootRun   # 실행
./gradlew test      # 테스트
./gradlew clean     # 정리
```

## 📚 문서

- [DOCKER.md](DOCKER.md) - Docker 사용 가이드
- [FALLACY_DETECTION_README.md](FALLACY_DETECTION_README.md) - 논리 오류 탐지 시스템 가이드
- [OPERATION_CHECKLIST.md](OPERATION_CHECKLIST.md) - 운영 준비 체크리스트
- [doc/README.md](doc/README.md) - API 다이어그램 목차

## 🛠️ 개발 환경 설정

### 데이터베이스 설정

`application.yml`에서 다음과 같이 설정되어 있습니다:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:13306/junglebook
    username: junglebook
    password: junglebook123!@#
```

### 환경 변수

개발 환경에서는 `application-dev.yml`을 사용합니다. 프로덕션 환경에서는 환경 변수를 통해 민감 정보를 관리하는 것을 권장합니다.

## 🐛 문제 해결

### 포트 충돌
만약 13306 또는 16379 포트가 이미 사용 중이라면, `docker/docker-compose.yml`에서 포트를 변경할 수 있습니다.

### 데이터 초기화
모든 데이터를 삭제하고 처음부터 시작하려면:
```bash
make docker-reset
```

⚠️ **주의**: 이 명령어는 모든 데이터를 삭제합니다!

자세한 문제 해결 방법은 [DOCKER.md](DOCKER.md)의 "문제 해결" 섹션을 참고하세요.

## 📄 라이선스

이 프로젝트는 개인 프로젝트입니다.

## 🤝 기여

프로젝트 개선을 위한 제안이나 버그 리포트는 이슈로 등록해주세요.
