# DebateArgumentController.createArgument - Class Diagram

```mermaid
classDiagram
    class DebateArgumentController {
        -DebateArgumentService debateArgumentService
        -MemberService memberService
        +createArgument(Member, Long, DebateArgumentCreateRequest) ResponseEntity
        -getMemberId(Member) Long
    }
    
    class DebateArgumentService {
        +createArgument(DebateArgumentEntity, List) DebateArgumentResponse
    }
    
    class MemberService {
        +findActivateMemberByLoginId(String) MemberEntity
    }
    
    class DebateArgumentRepository {
        +save(DebateArgumentEntity) DebateArgumentEntity
    }
    
    class DebateArgumentEntity {
        +Long id
        +Long topicId
        +Long userId
        +ArgumentStance stance
        +String content
        +Int viewCount
        +Int supportCount
        +Int opposeCount
        +Boolean activeYn
        +LocalDateTime createdAt
    }
    
    class DebateArgumentCreateRequest {
        +ArgumentStance stance
        +String content
        +List fileIds
    }
    
    class DebateArgumentResponse {
        +Long id
        +Long topicId
        +ArgumentStance stance
        +String content
        +Long userId
        +String authorNickname
        +LocalDateTime createdAt
    }
    
    class MemberEntity {
        +Long id
        +String loginId
        +String nickname
    }
    
    DebateArgumentController --> DebateArgumentService : uses
    DebateArgumentController --> MemberService : uses
    DebateArgumentService --> DebateArgumentRepository : uses
    DebateArgumentRepository --> DebateArgumentEntity : manages
    DebateArgumentService --> DebateArgumentEntity : creates
    DebateArgumentService --> DebateArgumentResponse : returns
    DebateArgumentController --> DebateArgumentCreateRequest : receives
    DebateArgumentController --> DebateArgumentResponse : returns
    DebateArgumentEntity --> MemberEntity : references
```

