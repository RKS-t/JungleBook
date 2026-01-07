# DebateTopicController.createTopic - Sequence Diagram

```mermaid
sequenceDiagram
    participant Client
    participant SecurityFilterChain
    participant JwtAuthenticationFilter
    participant DebateTopicController
    participant MemberService
    participant DebateTopicService
    participant DebateTopicRepository
    participant GlobalExceptionHandler
    
    Client->>SecurityFilterChain: POST /api/debate/topics
    SecurityFilterChain->>JwtAuthenticationFilter: Filter request
    JwtAuthenticationFilter->>JwtAuthenticationFilter: Extract JWT token
    JwtAuthenticationFilter->>JwtAuthenticationFilter: Validate token
    JwtAuthenticationFilter->>JwtAuthenticationFilter: Load Member from token
    JwtAuthenticationFilter->>DebateTopicController: Forward request with @AuthenticationPrincipal
    
    DebateTopicController->>DebateTopicController: createTopic(member, request)
    DebateTopicController->>MemberService: getMemberId(member)
    MemberService->>MemberService: findActivateMemberByLoginId(loginId)
    MemberService-->>DebateTopicController: memberId
    
    DebateTopicController->>DebateTopicService: createTopic(request, memberId)
    DebateTopicService->>DebateTopicService: toEntity(creatorId)
    DebateTopicService->>DebateTopicRepository: save(DebateTopicEntity)
    DebateTopicRepository-->>DebateTopicService: DebateTopicEntity
    DebateTopicService->>DebateTopicService: DebateTopicResponse.of(saved)
    DebateTopicService-->>DebateTopicController: DebateTopicResponse
    
    DebateTopicController-->>Client: 201 Created (DebateTopicResponse)
    
    alt Error occurs
        DebateTopicController->>GlobalExceptionHandler: Throw GlobalException
        GlobalExceptionHandler->>GlobalExceptionHandler: Map to HTTP status
        GlobalExceptionHandler-->>Client: Error response (400/401/403/404/500)
    end
```
