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
    participant DebateFileRepository
    participant DebateTopicRepository
    participant DebateTopicService
    participant FallacyDetectionService
    participant TransactionTemplate
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
    alt Content length exceeded
        DebateArgumentService->>GlobalExceptionHandler: Throw GlobalException(WRONG_ACCESS)
        GlobalExceptionHandler-->>Client: 400 Bad Request
    end
    DebateArgumentService->>DebateArgumentService: toEntity(topicId, userId)
    DebateArgumentService->>DebateArgumentRepository: save(DebateArgumentEntity)
    DebateArgumentRepository-->>DebateArgumentService: DebateArgumentEntity
    
    alt fileIds not empty
        loop For each fileId
            DebateArgumentService->>DebateFileRepository: updateAttachStatus(refType, refId, fileId, userId)
        end
    end
    
    DebateArgumentService->>DebateTopicService: increaseArgumentCount(topicId)
    DebateArgumentService->>DebateTopicRepository: findByIdAndActiveYnTrue(topicId)
    DebateTopicRepository-->>DebateArgumentService: DebateTopicEntity?
    
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
        DebateArgumentService->>TransactionTemplate: executeWithoutResult { ... }
        TransactionTemplate->>DebateArgumentRepository: findById(argumentId)
        DebateArgumentRepository-->>TransactionTemplate: Optional<DebateArgumentEntity>
        TransactionTemplate->>TransactionTemplate: Update fallacy fields
        TransactionTemplate->>DebateArgumentRepository: save(entity)
        TransactionTemplate->>DebateArgumentService: logger().info("Fallacy detection result saved")
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
