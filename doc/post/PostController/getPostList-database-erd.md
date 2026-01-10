# PostController.getPostList - Database ERD

```mermaid
erDiagram
    POST }o--|| BOARD : "belongs to"
    POST }o--|| MEMBER : "created by"
    
    POST {
        bigint id PK
        int board_id FK
        bigint user_id FK
        varchar title
        text content
        int view_cnt
        int like_cnt
        tinyint notice_yn
        tinyint use_yn
        datetime created_dt
        datetime updated_dt
    }
    
    BOARD {
        int id PK
        varchar name
        varchar description
    }
    
    MEMBER {
        bigint id PK
        varchar login_id UK
        varchar nickname
    }
```

## Table Relationships

- **POST** → **BOARD**: Many-to-One (게시글은 하나의 게시판에 속함)
- **POST** → **MEMBER**: Many-to-One (게시글은 하나의 회원이 작성)

## Sorting Options

- **LATEST**: 최신순 (notice_yn DESC, created_dt DESC)
- **POPULAR**: 인기순 ((like_cnt + view_cnt) DESC, created_dt DESC)
- **MOST_VIEWED**: 조회수순 (notice_yn DESC, view_cnt DESC, created_dt DESC)
- **MOST_LIKED**: 좋아요순 (notice_yn DESC, like_cnt DESC, created_dt DESC)

