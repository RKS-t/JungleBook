# PostController.getPostsByAuthor - Class Diagram

```mermaid
classDiagram
    class PostController {
        -PostQueryService postQueryService
        +getPostsByAuthor(Long, Int, Int) ResponseEntity~PostListResponse~
    }
    
    class PostQueryService {
        +getPostsByAuthor(Long, Int, Int) PostListResponse
    }
    
    class PostRepository {
        +findByUserIdAndUseYnTrueOrderByCreatedDtDesc(Long, Pageable) List~PostEntity~
        +countByUserIdAndUseYnTrue(Long) Long
    }
    
    class PostEntity {
        +Long id
        +Long userId
        +String title
        +String content
        +Boolean useYn
        +LocalDateTime createdDt
    }
    
    class PostListResponse {
        +Int totalCount
        +Int pageNo
        +List~Post~ posts
    }
    
    class Post {
        +Long id
        +String title
        +String authorNickname
        +LocalDateTime createdAt
    }
    
    PostController --> PostQueryService : uses
    PostQueryService --> PostRepository : uses
    PostRepository --> PostEntity : manages
    PostQueryService --> PostListResponse : returns
    PostListResponse --> Post : contains
```

