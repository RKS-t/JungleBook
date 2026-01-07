# JungleBook API Documentation

이 문서는 JungleBook 프로젝트의 API 엔드포인트에 대한 Mermaid 차트 문서입니다.

## 목차 (Table of Contents)

### Post APIs (게시판 API)

#### PostController
- [createPost - Class Diagram](post/PostController/createPost-class-diagram.md)
- [createPost - Sequence Diagram](post/PostController/createPost-sequence.md)
- [createPost - Flowchart](post/PostController/createPost-flowchart.md)
- [createPost - Redis Type Map](post/PostController/createPost-redis-typemap.md)
- [createPost - Database ERD](post/PostController/createPost-database-erd.md)

### Member APIs (회원 API)

#### MemberController
- [signupAndLogin - Class Diagram](MemberController/signupAndLogin-class-diagram.md)
- [signupAndLogin - Sequence Diagram](MemberController/signupAndLogin-sequence.md)
- [signupAndLogin - Flowchart](MemberController/signupAndLogin-flowchart.md)
- [signupAndLogin - Redis Type Map](MemberController/signupAndLogin-redis-typemap.md)
- [signupAndLogin - Database ERD](MemberController/signupAndLogin-database-erd.md)

### Debate APIs (토론 API)

#### DebateTopicController
- [createTopic - Class Diagram](debate/DebateTopicController/createTopic-class-diagram.md)
- [createTopic - Sequence Diagram](debate/DebateTopicController/createTopic-sequence.md)
- [createTopic - Flowchart](debate/DebateTopicController/createTopic-flowchart.md)
- [createTopic - Redis Type Map](debate/DebateTopicController/createTopic-redis-typemap.md)
- [createTopic - Database ERD](debate/DebateTopicController/createTopic-database-erd.md)

#### DebateArgumentController
- [createArgument - Class Diagram](debate/DebateArgumentController/createArgument-class-diagram.md)
- [createArgument - Sequence Diagram](debate/DebateArgumentController/createArgument-sequence.md)
- [createArgument - Flowchart](debate/DebateArgumentController/createArgument-flowchart.md)
- [createArgument - Redis Type Map](debate/DebateArgumentController/createArgument-redis-typemap.md)
- [createArgument - Database ERD](debate/DebateArgumentController/createArgument-database-erd.md)

## 차트 타입 설명

각 API 엔드포인트에 대해 다음 5가지 타입의 차트가 제공됩니다:

1. **Class Diagram**: 클래스 구조와 관계를 보여줍니다.
2. **Sequence Diagram**: 컴포넌트 간 상호작용 흐름을 보여줍니다.
3. **Flowchart**: 비즈니스 로직 흐름을 보여줍니다.
4. **Redis Type Map**: Redis 키 공간 구조와 데이터 타입을 보여줍니다.
5. **Database ERD**: 데이터베이스 스키마와 관계를 보여줍니다.

## 업데이트 내역

- 2024-12-15: 초기 문서 생성
  - PostController.createPost 차트 생성
  - MemberController.signupAndLogin 차트 생성
  - DebateTopicController.createTopic 차트 생성
  - DebateArgumentController.createArgument 차트 생성
  - Mermaid 문법 수정 (제네릭 표기 개선)
- 2024-12-15: 다이어그램 정확도 개선
  - 실제 코드 흐름 반영 (signupAndLogin의 즉시 로그인 로직)
  - GlobalExceptionHandler 명시적 추가
  - 불필요한 중복 제거 (토큰 생성 단계 통합)
  - 구현 세부사항 명시 (BCryptPasswordEncoder, JwtAuthenticationFilter 등)

