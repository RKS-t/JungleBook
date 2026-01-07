# DebateArgumentController.createArgument - Database ERD

```mermaid
erDiagram
    DEBATE_ARGUMENT ||--o{ DEBATE_REPLY : "has"
    DEBATE_ARGUMENT }o--|| DEBATE_TOPIC : "belongs to"
    DEBATE_ARGUMENT }o--|| MEMBER : "created by"
    DEBATE_ARGUMENT ||--o| DEBATE_ARGUMENT_FALLACY : "has"
    
    DEBATE_ARGUMENT {
        bigint id PK
        bigint topic_id FK
        bigint user_id FK
        enum stance
        text content
        int view_count
        int support_count
        int oppose_count
        tinyint fallacy_checked_yn
        tinyint active_yn
        datetime created_at
        datetime updated_at
    }
    
    DEBATE_TOPIC {
        bigint id PK
        bigint user_id FK
        varchar title
        text description
        enum category
        enum status
    }
    
    MEMBER {
        bigint id PK
        varchar login_id UK
        varchar nickname UK
    }
    
    DEBATE_REPLY {
        bigint id PK
        bigint argument_id FK
        bigint user_id FK
        text content
        tinyint active_yn
    }
    
    DEBATE_ARGUMENT_FALLACY {
        bigint id PK
        bigint argument_id FK
        tinyint has_fallacy
        varchar fallacy_type
        decimal confidence
    }
```

## Table Relationships

- **DEBATE_ARGUMENT** → **DEBATE_TOPIC**: Many-to-One (논증은 하나의 토픽에 속함)
- **DEBATE_ARGUMENT** → **MEMBER**: Many-to-One (논증은 하나의 회원이 작성)
- **DEBATE_ARGUMENT** → **DEBATE_REPLY**: One-to-Many (논증은 여러 댓글을 가질 수 있음)
- **DEBATE_ARGUMENT** → **DEBATE_ARGUMENT_FALLACY**: One-to-One (논증은 논리 오류 정보를 가질 수 있음)

