package org.example.junglebook.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import org.example.junglebook.entity.post.BoardEntity
import org.example.junglebook.entity.post.PostEntity
import org.example.junglebook.entity.post.PostFileEntity
import org.example.junglebook.entity.post.PostReplyEntity
import java.time.LocalDateTime


data class PostCreateRequest(
    val title: String? = null,
    val content: String? = null,
    val contentHtml: String? = null,
    val fileIds: List<Long>? = null
) {
    fun toEntity(board: BoardEntity, memberId: Long, nickname: String): PostEntity {
        return PostEntity(
            boardId = board.id!!,
            title = title ?: "",
            content = content ?: "",
            contentHtml = contentHtml,
            fileYn = !fileIds.isNullOrEmpty(),
            userId = memberId,  // MemberEntity의 id 사용
            nickname = nickname
        )
    }
}

data class PostUpdateRequest(
    val title: String? = null,
    val content: String? = null,
    val contentHtml: String? = null,
    val newFileIds: List<Long>? = null,
    val delFileIds: List<Long>? = null
) {
    fun toEntity(board: BoardEntity, memberId: Long): PostEntity {
        val fileYn = when {
            !newFileIds.isNullOrEmpty() -> true
            newFileIds == null && !delFileIds.isNullOrEmpty() -> false
            else -> null
        }

        return PostEntity(
            boardId = board.id!!,
            title = title ?: "",
            content = content ?: "",
            contentHtml = contentHtml,
            fileYn = fileYn ?: false,
            userId = memberId  // MemberEntity의 id 사용
        )
    }
}

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
        val seqNo: Long?,
        val noticeYn: Boolean,
        val fileYn: Boolean,
        val nickname: String?,
        val title: String,
        val view: Int,
        val like: Int,
        val dislike: Int,
        val reply: Int,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
        val createdDt: LocalDateTime
    ) {
        companion object {
            fun of(entity: PostEntity): Post {
                return Post(
                    id = entity.id,
                    seqNo = entity.seqNo,
                    noticeYn = entity.noticeYn,
                    fileYn = entity.fileYn,
                    nickname = entity.nickname,
                    title = entity.title,
                    view = entity.viewCnt,
                    like = entity.likeCnt,
                    dislike = entity.dislikeCnt,
                    reply = entity.replyCnt,
                    createdDt = entity.createdDt
                )
            }
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PostViewResponse(
    val id: Long?,
    val noticeYn: Boolean,
    val fileYn: Boolean,
    val nickname: String?,
    val title: String,
    val contentHtml: String?,
    val view: Int,
    val like: Int,
    val dislike: Int,
    val reply: Int,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdDt: LocalDateTime,

    val files: List<PostFileResponse> = emptyList(),
    val replies: List<PostReplyResponse> = emptyList()
) {
    companion object {
        fun of(entity: PostEntity, replies: List<PostReplyEntity>?): PostViewResponse {
            return PostViewResponse(
                id = entity.id,
                noticeYn = entity.noticeYn,
                fileYn = entity.fileYn,
                nickname = entity.nickname,
                title = entity.title,
                contentHtml = entity.contentHtml,
                view = entity.viewCnt,
                like = entity.likeCnt,
                dislike = entity.dislikeCnt,
                reply = entity.replyCnt,
                createdDt = entity.createdDt,
                files = entity.files.map { PostFileResponse.of(it) },
                replies = replies?.map { PostReplyResponse.of(it) } ?: emptyList()
            )
        }
    }
}