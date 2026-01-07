package org.example.junglebook.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import org.example.junglebook.entity.post.BoardEntity
import org.example.junglebook.entity.post.PostEntity
import org.example.junglebook.entity.post.PostFileEntity
import org.example.junglebook.entity.post.PostReplyEntity
import java.time.LocalDateTime


data class PostCreateRequest(
    val title: String,
    val content: String,
    val contentHtml: String? = null,
    val fileIds: List<Long>? = null,
    val authorNickname: String? = null
) {
    fun toEntity(boardId: Int, userId: Long, authorNickname: String): PostEntity {
        return PostEntity(
            boardId = boardId,
            title = title,
            content = content,
            contentHtml = contentHtml,
            fileYn = !fileIds.isNullOrEmpty(),
            userId = userId,
            authorNickname = authorNickname
        )
    }
}

data class PostUpdateRequest(
    val title: String? = null,
    val content: String? = null,
    val contentHtml: String? = null,
    val newFileIds: List<Long>? = null,
    val delFileIds: List<Long>? = null
)

// Response DTOs
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PostFileResponse(
    val id: Long? = null,
    val fileName: String? = null,
    val fileSize: String? = null,
    val url: String? = null
) {
    companion object {
        fun of(entity: PostFileEntity): PostFileResponse {
            return PostFileResponse(
                id = entity.id,
                fileName = entity.fileName,
                fileSize = entity.fileSize,
                url = entity.url
            )
        }

        fun of(url: String): PostFileResponse {
            return PostFileResponse(url = url)
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PostListResponse(
    val totalCount: Int,
    val pageNo: Int,
    val posts: List<Post>
) {
    companion object {
        fun of(totalCount: Int, pageNo: Int, entities: List<PostEntity>?): PostListResponse {
            return PostListResponse(
                totalCount = totalCount,
                pageNo = pageNo,
                posts = entities?.map { Post.of(it) } ?: emptyList()
            )
        }
    }

    data class Post(
        val id: Long?,
        val noticeYn: Boolean,
        val fileYn: Boolean,
        val authorNickname: String,
        val title: String,
        val viewCnt: Int,
        val likeCnt: Int,
        val replyCnt: Int,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
        val createdAt: LocalDateTime
    ) {
        companion object {
            fun of(entity: PostEntity): Post {
                return Post(
                    id = entity.id,
                    noticeYn = entity.noticeYn,
                    fileYn = entity.fileYn,
                    authorNickname = entity.authorNickname,
                    title = entity.title,
                    viewCnt = entity.viewCnt,
                    likeCnt = entity.likeCnt,
                    replyCnt = entity.replyCnt,
                    createdAt = entity.createdDt
                )
            }
        }
    }
}

// Response DTOs 추가
data class PostResponse(
    val id: Long,
    val boardId: Int,
    val userId: Long,
    val authorNickname: String,
    val title: String,
    val content: String,
    val contentHtml: String?,
    val noticeYn: Boolean,
    val fileYn: Boolean,
    val viewCnt: Int,
    val likeCnt: Int,
    val replyCnt: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun of(entity: PostEntity): PostResponse {
            return PostResponse(
                id = entity.id!!,
                boardId = entity.boardId,
                userId = entity.userId ?: 0L,
                authorNickname = entity.authorNickname,
                title = entity.title,
                content = entity.content,
                contentHtml = entity.contentHtml,
                noticeYn = entity.noticeYn,
                fileYn = entity.fileYn,
                viewCnt = entity.viewCnt,
                likeCnt = entity.likeCnt,
                replyCnt = entity.replyCnt,
                createdAt = entity.createdDt,
                updatedAt = entity.updatedDt
            )
        }
    }
}

data class PostSimpleResponse(
    val id: Long,
    val title: String,
    val authorNickname: String,
    val viewCnt: Int,
    val likeCnt: Int,
    val replyCnt: Int,
    val createdAt: LocalDateTime
) {
    companion object {
        fun of(entity: PostEntity): PostSimpleResponse {
            return PostSimpleResponse(
                id = entity.id!!,
                title = entity.title,
                authorNickname = entity.authorNickname,
                viewCnt = entity.viewCnt,
                likeCnt = entity.likeCnt,
                replyCnt = entity.replyCnt,
                createdAt = entity.createdDt
            )
        }

        fun of(entities: List<PostEntity>): List<PostSimpleResponse> {
            return entities.map { of(it) }
        }
    }
}

data class PostDetailResponse(
    val post: PostResponse,
    val files: List<PostFileResponse> = emptyList()
)

enum class PostSortType {
    LATEST,      // 최신순
    POPULAR,     // 인기순 (좋아요 + 조회수)
    MOST_VIEWED, // 조회수순
    MOST_LIKED   // 좋아요순
}