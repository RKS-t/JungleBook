package org.example.junglebook.enums.post

import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class Board(
    val id: Int,
    val boardName: String,
    val buttonName: String
) {
    POPULAR(1, "popular", "인기게시판"),
    FREE(2, "free", "자유게시판"),
    HUMOR(3, "humor", "유머게시판"),
    POLITICS(4, "politics", "정치게시판"),
    QNA(5, "qna", "질문게시판");

    companion object {
        fun getIdByBoardName(boardName: String): Int {
            return values().find { it.boardName == boardName }?.id ?: 0
        }
    }
}