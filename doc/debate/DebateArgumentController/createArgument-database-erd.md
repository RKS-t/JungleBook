# DebateArgumentController.createArgument - Database ERD

```mermaid
erDiagram
    DEBATE_ARGUMENT ||--o{ DEBATE_REPLY : "has"
    DEBATE_ARGUMENT }o--|| DEBATE_TOPIC : "belongs to"
    DEBATE_ARGUMENT }o--|| MEMBER : "created by"
    DEBATE_ARGUMENT ||--o{ DEBATE_FILE : "has"
    
    DEBATE_ARGUMENT {
        bigint id PK
        bigint topic_id FK
        bigint user_id FK
        enum stance
        varchar title
        text content
        text content_html
        varchar author_nickname
        int view_count
        int support_count
        int oppose_count
        int reply_count
        boolean fallacy_has_fallacy
        varchar fallacy_type
        double fallacy_confidence
        text fallacy_explanation
        boolean fallacy_checked_yn
        boolean active_yn
        boolean notice_yn
        boolean file_yn
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
        text content_html
        varchar author_nickname
        int support_count
        int oppose_count
        boolean active_yn
        boolean file_yn
        datetime created_at
        datetime updated_at
    }
    
    DEBATE_FILE {
        bigint id PK
        int ref_type
        bigint ref_id
        bigint user_id FK
        varchar file_name
        varchar file_size
        varchar url
        boolean attach_yn
        datetime created_at
    }
```

## Table Relationships

- **DEBATE_ARGUMENT** → **DEBATE_TOPIC**: Many-to-One (논증은 하나의 토픽에 속함)
- **DEBATE_ARGUMENT** → **MEMBER**: Many-to-One (논증은 하나의 회원이 작성)
- **DEBATE_ARGUMENT** → **DEBATE_REPLY**: One-to-Many (논증은 여러 댓글을 가질 수 있음)
- **DEBATE_ARGUMENT** → **DEBATE_FILE**: One-to-Many (논증은 여러 파일을 가질 수 있음)
- **DEBATE_ARGUMENT**: 논리 오류 정보는 테이블 내부 필드로 관리됨 (fallacy_has_fallacy, fallacy_type, fallacy_confidence, fallacy_explanation)

