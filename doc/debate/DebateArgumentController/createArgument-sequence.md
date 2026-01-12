# DebateArgumentController.createArgument - Sequence Diagram

```mermaid
sequenceDiagram
    participant Client
    participant SecurityFilterChain
    participant JwtAuthenticationFilter
    participant DebateArgumentController
    participant MemberService
    participant DebateArgumentService
    participant DebateArgumentRepository
    participant FallacyDetectionService
    participant GlobalExceptionHandler
    
    Client->>SecurityFilterChain: POST /api/debate/topics/:topicId/arguments
    SecurityFilterChain->>JwtAuthenticationFilter: Filter request
    JwtAuthenticationFilter->>JwtAuthenticationFilter: Extract JWT token
    JwtAuthenticationFilter->>JwtAuthenticationFilter: Validate token
    JwtAuthenticationFilter->>JwtAuthenticationFilter: Load Member from token
    JwtAuthenticationFilter->>DebateArgumentController: Forward request with @AuthenticationPrincipal
    
    DebateArgumentController->>DebateArgumentController: createArgument(member, topicId, request)
    DebateArgumentController->>MemberService: getMemberId(member)
    MemberService->>MemberService: findActivateMemberByLoginId(loginId)
    MemberService-->>DebateArgumentController: memberId
    
    DebateArgumentController->>DebateArgumentService: createArgument(topicId, memberId, request)
    DebateArgumentService->>DebateArgumentService: Validate content length
    DebateArgumentService->>DebateArgumentService: toEntity(topicId, userId)
    DebateArgumentService->>DebateArgumentRepository: save(DebateArgumentEntity)
    DebateArgumentRepository-->>DebateArgumentService: DebateArgumentEntity
    
    Note over DebateArgumentService,FallacyDetectionService: Asynchronous fallacy detection with timeout
    DebateArgumentService->>FallacyDetectionService: detectFallacyAsync(content, topicTitle, topicDescription)
    FallacyDetectionService->>FallacyDetectionService: supplyAsync { detectFallacy(...) }
    FallacyDetectionService->>FallacyDetectionService: orTimeout(timeout, TimeUnit.MILLISECONDS)
    FallacyDetectionService-->>DebateArgumentService: CompletableFuture<FallacyResult?>
    
    DebateArgumentService-->>DebateArgumentController: DebateArgumentResponse (immediate return)
    DebateArgumentController-->>Client: 201 Created (DebateArgumentResponse)
    
    Note over DebateArgumentService: Async callback processing (non-blocking)
    DebateArgumentService->>DebateArgumentService: thenAccept { result -> ... }
    alt Fallacy detection success
        DebateArgumentService->>DebateArgumentService: transactionTemplate.executeWithoutResult
        DebateArgumentService->>DebateArgumentRepository: findById(argumentId)
        DebateArgumentRepository-->>DebateArgumentService: DebateArgumentEntity
        DebateArgumentService->>DebateArgumentService: Update fallacy fields
        DebateArgumentService->>DebateArgumentRepository: save(entity)
        DebateArgumentService->>DebateArgumentService: logger().info("Fallacy detection result saved")
    else TimeoutException
        DebateArgumentService->>DebateArgumentService: logger().warn("Fallacy detection timeout")
    else Other exception
        DebateArgumentService->>DebateArgumentService: logger().error("Failed to save fallacy detection result")
    end
    
    alt Error occurs
        DebateArgumentController->>GlobalExceptionHandler: Throw GlobalException
        GlobalExceptionHandler->>GlobalExceptionHandler: Map to HTTP status
        GlobalExceptionHandler-->>Client: Error response (400/401/403/404/500)
    end
```
