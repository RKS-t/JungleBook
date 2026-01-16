# DebateTopicController.createTopic - Class Diagram

```mermaid
classDiagram
    class DebateTopicController {
        -DebateTopicCommandService debateTopicCommandService
        -MemberService memberService
        +createTopic(Member, DebateTopicCreateRequest) ResponseEntity
        -getMemberId(Member) Long
    }
    
    class DebateTopicCommandService {
        +createTopic(DebateTopicCreateRequest, Long) DebateTopicResponse
    }
    
    class MemberService {
        +getMemberId(Member) Long
    }
    
    class DebateTopicRepository {
        +save(DebateTopicEntity) DebateTopicEntity
    }
    
    class DebateTopicEntity {
        +Long id
        +Long creatorId
        +String title
        +String description
        +DebateTopicCategory category
        +DebateTopicStatus status
        +LocalDateTime startDate
        +LocalDateTime endDate
        +Int viewCount
        +Int argumentCount
        +Boolean activeYn
        +LocalDateTime createdAt
    }
    
    class DebateTopicCreateRequest {
        +String title
        +String description
        +DebateTopicCategory category
        +LocalDateTime startDate
        +LocalDateTime endDate
    }
    
    class DebateTopicResponse {
        +Long id
        +String title
        +String description
        +DebateTopicCategory category
        +DebateTopicStatus status
        +LocalDateTime createdAt
    }
    
    DebateTopicController --> DebateTopicCommandService : uses
    DebateTopicController --> MemberService : uses
    DebateTopicCommandService --> DebateTopicRepository : uses
    DebateTopicRepository --> DebateTopicEntity : manages
    DebateTopicCommandService --> DebateTopicEntity : creates
    DebateTopicCommandService --> DebateTopicResponse : returns
    DebateTopicController --> DebateTopicCreateRequest : receives
    DebateTopicController --> DebateTopicResponse : returns
```

