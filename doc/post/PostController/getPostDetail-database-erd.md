# PostController.getPostDetail - Database ERD

```mermaid
erDiagram
    POST ||--o{ POST_FILE : "has"
    POST }o--|| MEMBER : "created by"
    
    POST {
        bigint id PK
        bigint user_id FK
        int board_id FK
        varchar title
        text content
        int view_cnt
        int like_cnt
        tinyint use_yn
        datetime created_dt
        datetime updated_dt
    }
    
    POST_FILE {
        bigint id PK
        int ref_type
        bigint ref_id
        bigint user_id FK
        varchar file_name
        varchar file_size
        varchar url
        tinyint attach_yn
        datetime created_at
    }
    
    MEMBER {
        bigint id PK
        varchar login_id UK
        varchar nickname
        varchar email
    }
```

## Table Relationships

- **POST** → **POST_FILE**: One-to-Many (게시글은 여러 파일을 가질 수 있음, ref_type=1, ref_id=post.id)
- **POST** → **MEMBER**: Many-to-One (게시글은 하나의 회원이 작성)

