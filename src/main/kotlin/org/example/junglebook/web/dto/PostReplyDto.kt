package org.example.junglebook.web.dto



import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotEmpty
import org.example.junglebook.entity.post.PostReplyEntity
import org.jsoup.Jsoup
import java.time.LocalDateTime

// Request DTOs
data class PostReplyCreateRequest(
    val pid: Long = 0,

    @field:NotEmpty
    val contentHtml: String? = null,

    val fileIds: List<Long>? = null
) {
    fun toEntity(boardId: Int, postId: Long, memberId: Long, nickname: String): PostReplyEntity {
        return PostReplyEntity(
            boardId = boardId,
            postId = postId,
            pid = if (pid == 0L) null else pid,
            content = contentHtml?.let { Jsoup.parse(it).body().text() } ?: "",
            contentHtml = contentHtml ?: "",
            fileYn = !fileIds.isNullOrEmpty(),
            userId = memberId,  // MemberEntity의 id 사용
            nickname = nickname
        )
    }
}

data class PostReplyUpdateRequest(
    @field:NotEmpty
    val contentHtml: String? = null,

    val fileIds: List<Long>? = null
) {
    fun toEntity(boardId: Int, postId: Long, id: Long, memberId: Long, nickname: String): PostReplyEntity {
        return PostReplyEntity(
            id = id,
            boardId = boardId,
            postId = postId,
            content = contentHtml?.let { Jsoup.parse(it).body().text() } ?: "",
            contentHtml = contentHtml ?: "",
            fileYn = false,
            userId = memberId,  // MemberEntity의 id 사용
            nickname = nickname
        )
    }
}

// Response DTOs
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PostReplyResponse(
    val id: Long?,
    val pid: Long?,
    val userId: Long?,  // MemberEntity의 id 참조
    val useYn: Boolean?,
    val fileYn: Boolean?,
    val nickname: String?,
    val content: String?,
    val contentHtml: String?,
    val like: Int,
    val dislike: Int,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdDt: LocalDateTime,

    val files: List<PostFileResponse> = emptyList()
) {
    companion object {
        fun of(entity: PostReplyEntity): PostReplyResponse {
            return PostReplyResponse(
                id = entity.id,
                pid = entity.pid,
                userId = entity.userId,  // MemberEntity의 id
                useYn = entity.useYn,
                fileYn = entity.fileYn,
                nickname = entity.nickname,
                content = entity.content,
                contentHtml = entity.contentHtml,
                like = entity.likeCnt,
                dislike = entity.dislikeCnt,
                createdDt = entity.createdDt,
                files = entity.files.map { PostFileResponse.of(it) }
            )
        }
    }
}