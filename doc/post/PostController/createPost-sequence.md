# PostController.createPost - Sequence Diagram

```mermaid
sequenceDiagram
    participant Client
    participant SecurityFilterChain
    participant JwtAuthenticationFilter
    participant PostController
    participant MemberService
    participant PostCommandService
    participant PostRepository
    participant PostFileRepository
    participant GlobalExceptionHandler
    
    Client->>SecurityFilterChain: POST /api/posts?boardId=1
    SecurityFilterChain->>JwtAuthenticationFilter: Filter request
    JwtAuthenticationFilter->>JwtAuthenticationFilter: Extract JWT token
    JwtAuthenticationFilter->>JwtAuthenticationFilter: Validate token
    JwtAuthenticationFilter->>JwtAuthenticationFilter: Load Member from token
    JwtAuthenticationFilter->>PostController: Forward request with @AuthenticationPrincipal
    
    PostController->>PostController: createPost(member, boardId, request)
    PostController->>MemberService: getMemberId(member)
    MemberService-->>PostController: memberId
    
    PostController->>PostCommandService: createPost(boardId, request, memberId)
    PostCommandService->>PostRepository: save(PostEntity)
    PostRepository-->>PostCommandService: PostEntity
    alt request.fileIds exists
        PostCommandService->>PostFileRepository: updateAttachStatus(refType, refId, fileId, userId)
        PostFileRepository-->>PostCommandService: updated
    end
    PostCommandService-->>PostController: PostResponse
    
    PostController-->>Client: 201 Created (PostResponse)
    
    alt Error occurs
        PostController->>GlobalExceptionHandler: Throw GlobalException
        GlobalExceptionHandler->>GlobalExceptionHandler: Map to HTTP status
        GlobalExceptionHandler-->>Client: Error response (400/401/403/404/500)
    end
```
