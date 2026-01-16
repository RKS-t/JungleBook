# PostController.getPostList - Sequence Diagram

```mermaid
sequenceDiagram
    participant Client
    participant SecurityFilterChain
    participant JwtAuthenticationFilter
    participant PostController
    participant PostQueryService
    participant PostRepository
    participant GlobalExceptionHandler
    
    Client->>SecurityFilterChain: GET /api/posts?boardId=1&sortType=LATEST&pageNo=0&limit=20
    SecurityFilterChain->>JwtAuthenticationFilter: Filter request
    JwtAuthenticationFilter->>JwtAuthenticationFilter: Extract JWT token
    JwtAuthenticationFilter->>JwtAuthenticationFilter: Validate token
    JwtAuthenticationFilter->>JwtAuthenticationFilter: Load Member from token
    JwtAuthenticationFilter->>PostController: Forward request with @AuthenticationPrincipal
    
    PostController->>PostController: getPostList(boardId, sortType, pageNo, limit, keyword)
    PostController->>PostQueryService: getPostList(boardId, sortType, pageNo, limit, keyword)
    
    PostQueryService->>PostQueryService: create PageRequest(pageNo, limit)
    
    alt sortType is LATEST
        alt keyword exists
            PostQueryService->>PostRepository: searchByKeyword(boardId, keyword, pageable)
        else keyword is null
            PostQueryService->>PostRepository: findByBoardIdAndUseYnTrueOrderByNoticeYnDescCreatedDtDesc(boardId, pageable)
        end
    else sortType is POPULAR
        PostQueryService->>PostRepository: findPopularByBoardId(boardId, pageable)
    else sortType is MOST_VIEWED
        PostQueryService->>PostRepository: findByBoardIdAndUseYnTrueOrderByViewCntDesc(boardId, pageable)
    else sortType is MOST_LIKED
        PostQueryService->>PostRepository: findByBoardIdAndUseYnTrueOrderByLikeCntDesc(boardId, pageable)
    end
    
    PostRepository-->>PostQueryService: List~PostEntity~
    
    PostQueryService->>PostRepository: countByBoardIdAndUseYnTrue(boardId)
    PostRepository-->>PostQueryService: Long
    
    PostQueryService->>PostQueryService: create PostListResponse
    PostQueryService-->>PostController: PostListResponse
    
    PostController-->>Client: 200 OK (PostListResponse)
    
    alt Error occurs
        PostController->>GlobalExceptionHandler: Throw exception
        GlobalExceptionHandler->>GlobalExceptionHandler: Map to HTTP status
        GlobalExceptionHandler-->>Client: Error response (400/500)
    end
```

