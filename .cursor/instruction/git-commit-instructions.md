### 🧠 Git Commit Message Generator (AI Auto-Analysis)

현재 staged된 변경사항(git diff)을 분석해서 커밋 메시지를 자동 생성해줘.
**결과는 반드시 코드블록(```) 안에 커밋 메시지만 출력.**

---

#### 📥 입력 형식
**Type:** feat (또는 생략 시 자동 판단)  
**JIRA:** WV2Q-1234 (없으면 생략)

---

#### 🤖 AI 분석 절차
1. **Type 결정:**
  - 사용자가 입력하면 → 그대로 사용
  - 입력 없으면 → 변경 내용 분석해서 자동 판단
2. **변경 파일 확인:** 어떤 파일들이 수정되었는지
3. **핵심 변경사항 추출:** 가장 중요한 변경 1-2개만 선택
4. **영향 범위 파악:** API 변경, 버그 수정, 리팩토링 등
5. **Breaking Change 체크:** API 스펙 변경, 타입 변경 등
6. **커밋 메시지 생성:** 위 분석을 바탕으로 작성

---

#### 🎯 핵심 규칙
1. **형식:** `<type>(<JIRA-ISSUE>): <subject>`
2. **제목:** 72자 이내, 명령형 현재 시제 (Add/추가, Fix/수정)
3. **본문:** 복잡한 변경이나 이유 설명이 필요한 경우에만 추가
4. **Footer:** Breaking Change, 다중 이슈 참조 시 추가
5. **언어:** 영어 또는 한글 중 하나로 통일 (프로젝트 컨벤션 따름)

---

#### 📘 Type 목록

| Type | 설명 | 자동 판단 키워드 |
|------|------|----------------|
| **feat** | 새 기능 추가 | 새 클래스/메서드, `@PostMapping`, `@GetMapping` |
| **fix** | 버그 수정 | `try-catch` 추가, null 체크, 조건문 수정 |
| **refactor** | 코드 개선 (기능 변화 없음) | 메서드 분리, 변수명 변경, 중복 제거 |
| **perf** | 성능 개선 | `@Cacheable`, 인덱스 추가, 쿼리 최적화 |
| **test** | 테스트 추가/수정 | `*Test.java`, `@Test`, mock 설정 |
| **docs** | 문서 변경 | `@ApiOperation`, README.md, 주석 |
| **style** | 코드 스타일 (포매팅) | 공백, 들여쓰기, import 순서 |
| **chore** | 빌드, 설정, 의존성 | `pom.xml`, `gradle`, `.properties` |
| **revert** | 커밋 되돌림 | git revert 사용 시 |

---

#### 🔍 제목(Subject) 작성 가이드

**AI가 변경 내용을 보고 자동으로 작성할 때:**

1. **가장 중요한 변경 1개만** 제목에 포함
2. **구체적으로** 작성 (모호한 "수정", "변경" 금지)
3. **기술 용어 사용** (예: "비동기 처리", "캐시", "인덱스")
4. **동사로 시작** (Add, Fix, Update, Remove 등)

**좋은 예시:**
```
✅ feat(WV2Q-123): add async email lookup endpoint
✅ fix(WV2Q-123): prevent NPE in getUserById when user not found
✅ refactor(WV2Q-123): extract validation logic to separate service
✅ perf(WV2Q-123): add Redis cache for user profile queries
✅ test(WV2Q-123): fix MockServerWebExchange immutability issue
```

**나쁜 예시:**
```
❌ feat(WV2Q-123): update user service
❌ fix(WV2Q-123): fix bug
❌ refactor(WV2Q-123): code improvement
❌ chore(WV2Q-123): modify files
```

---

#### 📝 본문(Body) 작성 판단

**다음 경우에만 본문 추가:**

1. **여러 파일/클래스 수정:** 각 변경사항을 불렛으로 정리
2. **Breaking Change:** API 스펙 변경, 필수 파라미터 추가 등
3. **복잡한 버그 수정:** 원인과 해결 방법 설명
4. **성능 개선:** 개선 전후 비교 (예: 500ms → 50ms)
5. **마이그레이션:** 버전 업그레이드, 프레임워크 변경

**본문 형식:**
```
<제목>

<빈 줄>

- 변경사항 1 설명
- 변경사항 2 설명
- 변경사항 3 설명

<필요시 추가 설명>
```

---

#### 🔧 Breaking Change 자동 감지

**다음 변경이 있으면 Breaking Change:**

1. API 응답 구조 변경 (필드 삭제/타입 변경)
2. 메서드 시그니처 변경 (파라미터 추가/삭제)
3. 기본값 변경
4. Enum 값 변경/삭제
5. URL 경로 변경

**표시 방법:**
```
feat(WV2Q-123)!: change user API response format

BREAKING CHANGE: userId 타입이 String → Long으로 변경됨
기존 API 클라이언트는 타입 캐스팅 필요
```

---

#### 📋 완성 예시

**예시 1: Type 입력 O**
```
입력:
Type: test
JIRA: WV2Q-123

변경: UserServiceTest.java에 ArgumentCaptor 추가

생성 결과:
```
```java
test(WV2Q-123): fix MockServerWebExchange immutability issue

Spring Boot 3.5.6 마이그레이션으로 인한 테스트 실패 수정
- ArgumentCaptor로 변경된 exchange 캡처
- mutate().build() 패턴으로 변경
```
```

**예시 2: Type 입력 X (자동 판단)**
```
입력:
JIRA: WV2Q-789
변경: UserController.java에 새 API 엔드포인트 추가

AI 판단: @PostMapping 발견 → feat
생성 결과: feat(WV2Q-789): add bulk email lookup API

POST /v1.0/users/emails 엔드포인트 추가
- 최대 100개 이메일 동시 조회
- CompletableFuture 기반 비동기 처리
- 5초 타임아웃 설정
```
```

**예시 3: Type만 입력 (JIRA 없음)**
```
입력:
Type: refactor

변경: UserService.java 메서드 분리

생성 결과:
```
```java
refactor: extract header setting logic to separate method

addUserAuthInfoHeader() 메서드 분리로 코드 가독성 향상
- 100줄 메서드를 3개로 분할
- 단일 책임 원칙 적용
```
```

**예시 4: 둘 다 입력 X (완전 자동)**
```
입력:
(변경사항만 분석)

변경: UserService.java에 null 체크 추가

AI 판단:
- Type: fix (버그 수정)
- JIRA 없음

생성 결과: fix: prevent NPE when user not found

getUserById() 메서드에서 Optional.orElseThrow() 추가
존재하지 않는 사용자 조회 시 UserNotFoundException 발생
```
```

**예시 5: Breaking Change (자동 감지)**
```
입력:
Type: feat
JIRA: WV2Q-890

변경: UserDTO.java의 userId 타입 변경 (String → Long)

AI 판단: 타입 변경 감지 → Breaking Change

생성 결과:
```
```java
feat(WV2Q-890)!: change userId type from String to Long

BREAKING CHANGE: UserDTO.userId가 String → Long으로 변경
- 데이터베이스 스키마와 일치
- API 응답 JSON의 userId가 숫자로 반환됨
- 기존 클라이언트는 파싱 로직 수정 필요
```
```

---

#### 🎨 입력 조합 패턴

| Type | JIRA | 동작 | 예시 |
|------|------|------|------|
| ✅ | ✅ | 입력값 사용 | `feat(WV2Q-123): add API` |
| ✅ | ❌ | Type만 사용 | `feat: add API` |
| ❌ | ✅ | Type 자동 판단 | `fix(WV2Q-123): prevent NPE` |
| ❌ | ❌ | 완전 자동 | `fix: prevent NPE` |

---

#### ⚠️ 금지 사항
- 마침표(.) 사용 금지 (제목 끝)
- 과거형 금지 (Added → Add, Fixed → Fix)
- 불필요한 수식어 (매우, 정말, 완전히)
- 모호한 표현 (수정함, 업데이트함 → 구체적으로)
- "및", "그리고" 남발 (1개 핵심 변경만)

---

#### 💡 사용 방법

**패턴 1: Type + JIRA 입력**
```
Type: test
JIRA: WV2Q-1234
```

**패턴 2: JIRA만 입력 (Type 자동 판단)**
```
JIRA: WV2Q-1234
```

**패턴 3: Type만 입력**
```
Type: refactor
```

**패턴 4: 완전 자동**
```
(아무것도 입력 안함)