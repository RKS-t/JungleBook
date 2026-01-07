# PostController.createPost - Database ERD

```mermaid
erDiagram
    POST ||--o{ POST_FILE : "has"
    POST }o--|| BOARD : "belongs to"
    POST }o--|| MEMBER : "created by"
    
    POST {
        bigint id PK
        int board_id FK
        bigint user_id FK
        varchar title
        text content
        int view_count
        int like_count
        tinyint use_yn
        datetime created_at
        datetime updated_at
    }
    
    BOARD {
        int id PK
        varchar name
        varchar description
        tinyint use_yn
    }
    
    MEMBER {
        bigint id PK
        varchar login_id UK
        varchar nickname UK
        varchar email UK
        varchar password
        tinyint delete_yn
    }
    
    POST_FILE {
        bigint id PK
        bigint post_id FK
        varchar file_name
        varchar file_path
        bigint file_size
        tinyint use_yn
    }
```

## Table Relationships

- **POST** → **BOARD**: Many-to-One (게시글은 하나의 게시판에 속함)
- **POST** → **MEMBER**: Many-to-One (게시글은 하나의 회원이 작성)
- **POST** → **POST_FILE**: One-to-Many (게시글은 여러 파일을 가질 수 있음)

