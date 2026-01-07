# DebateTopicController.createTopic - Database ERD

```mermaid
erDiagram
    DEBATE_TOPIC ||--o{ DEBATE_ARGUMENT : "has"
    DEBATE_TOPIC }o--|| MEMBER : "created by"
    
    DEBATE_TOPIC {
        bigint id PK
        bigint user_id FK
        varchar title
        text description
        enum category
        enum status
        datetime start_date
        datetime end_date
        int view_count
        int argument_count
        tinyint active_yn
        datetime created_at
        datetime updated_at
    }
    
    MEMBER {
        bigint id PK
        varchar login_id UK
        varchar nickname UK
        varchar email UK
    }
    
    DEBATE_ARGUMENT {
        bigint id PK
        bigint topic_id FK
        bigint user_id FK
        enum stance
        text content
        int view_count
        int support_count
        int oppose_count
        tinyint active_yn
    }
```

## Table Relationships

- **DEBATE_TOPIC** → **MEMBER**: Many-to-One (토픽은 하나의 회원이 생성)
- **DEBATE_TOPIC** → **DEBATE_ARGUMENT**: One-to-Many (토픽은 여러 논증을 가질 수 있음)

