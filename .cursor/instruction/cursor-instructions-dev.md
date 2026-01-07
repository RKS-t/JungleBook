## 적용 조건
- 이 지시사항은 **API 개발 또는 수정**,  
  또는 **API 관련 테스트 코드 작성/수정 시에만** 적용합니다.
- 일반 문서 작성, 주석 정리, 설정 파일 변경, UI 코드 작성 등에는 **적용하지 않습니다.**

## General Guidelines / 일반 지침
- Follow **Kotlin** and **Spring Boot** conventions. (Kotlin 및 Spring Boot 규칙을 따르세요)
- Keep code consistent with **clean architecture** and **layered separation**. (클린 아키텍처와 계층 구조를 유지하세요)
- Avoid unnecessary abstractions; prefer readability and simplicity. (불필요한 추상화는 피하고, 가독성과 단순성을 우선하세요)
- No comments, markdown, or documentation output. (불필요한 주석, 마크다운, 문서 출력 금지)

## Naming / 네이밍
- Use **camelCase** for variables and methods. (변수와 메서드는 camelCase 사용)
- Use **PascalCase** for classes, enums, and data classes. (클래스, enum, data class는 PascalCase 사용)
- Use **UPPER_SNAKE_CASE** for constants. (상수는 UPPER_SNAKE_CASE 사용)
- Keep method names descriptive but concise. (메서드 이름은 설명적이면서 간결하게)

## Coding Practices / 코딩 규칙
- Use `val` by default; use `var` only when mutation is necessary. (기본적으로 val 사용, 변경이 필요한 경우에만 var 사용)
- Prefer immutable data structures. (불변 자료구조 선호)
- Add null checks using `requireNotNull()`, `checkNotNull()`, or safe call operator `?.`. (필요 시 requireNotNull(), checkNotNull(), 또는 안전 호출 연산자 `?.` 사용)
- Prefer **constructor injection** (Kotlin's primary constructor). (생성자 주입 선호 - Kotlin의 주 생성자 사용)
- Use **data classes** for value objects instead of Lombok. (값 객체는 Lombok 대신 data class 사용)
- Prefer Kotlin collection functions (map, filter, etc.) over Java streams, but prioritize clarity. (Java 스트림보다 Kotlin 컬렉션 함수(map, filter 등) 선호, 가독성 우선)

## Performance / 성능 최적화
- Avoid unnecessary database calls; prefer batch or bulk operations. (불필요한 DB 호출은 피하고, 배치/벌크 작업 선호)
- Use efficient data structures and algorithms. (효율적인 자료구조와 알고리즘 사용)
- Avoid excessive object creation inside loops or hot paths. (핫 루프 내 불필요한 객체 생성 금지)
- Prefer coroutines for asynchronous operations where appropriate, but maintain readability. (필요 시 코루틴 사용, 가독성 유지)

## Error Handling / 에러 처리
- Throw meaningful custom exceptions. (의미 있는 커스텀 예외를 사용)
- Log exceptions appropriately (WARN/ERROR). (예외를 적절히 로깅)
- Never swallow exceptions silently. (예외를 묵살하지 않음)
- Use `require()`, `check()`, or `requireNotNull()` for precondition validation. (전제 조건 검증에는 require(), check(), requireNotNull() 사용)

## Testing Guidelines / 테스트 지침
- Always create corresponding **JUnit 5 test classes**. (각 서비스/유틸 클래스에 JUnit 5 테스트 생성)
- Use **Mockito** for mocking and **AssertJ** for assertions. (Mockito 및 AssertJ 사용)
- Follow **given–when–then** pattern in test methods. (given–when–then 구조)
- Test both **happy paths** and **failure scenarios**. (성공/실패 케이스 모두 테스트)
- Use **@SpringBootTest** only if context loading is necessary. (@SpringBootTest는 컨텍스트 필요 시만 사용)
- Use Kotlin's `@Test` annotation from JUnit 5. (JUnit 5의 @Test 어노테이션 사용)

