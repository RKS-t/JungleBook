package org.example.junglebook.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import org.example.junglebook.entity.debate.JunglebookEntity
import org.example.junglebook.entity.debate.JunglebookPostEntity
import java.time.LocalDate
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JunglebookListResponse(
    val totalCount: Int? = null,
    val pageNo: Int? = null,
    val junglebooks: List<JunglebookResponse>
) {
    companion object {
        fun of(entities: List<JunglebookEntity>?): JunglebookListResponse {
            return JunglebookListResponse(
                junglebooks = entities?.map { JunglebookResponse.of(it) } ?: emptyList()
            )
        }

        fun of(totalCount: Int, pageNo: Int, entities: List<JunglebookEntity>?): JunglebookListResponse {
            return JunglebookListResponse(
                totalCount = totalCount,
                pageNo = pageNo,
                junglebooks = entities?.map { JunglebookResponse.of(it) } ?: emptyList()
            )
        }
    }
}

data class JunglebookPostListResponse(
    val totalCount: Int,
    val pageNo: Int,
    val posts: List<JunglebookPostSimpleResponse>
) {
    companion object {
        fun of(totalCount: Int, pageNo: Int, entities: List<JunglebookPostEntity>?): JunglebookPostListResponse {
            return JunglebookPostListResponse(
                totalCount = totalCount,
                pageNo = pageNo,
                posts = JunglebookPostSimpleResponse.of(entities)
            )
        }
    }
}

data class JunglebookPostResponse(
    val id: Long?,
    val tid: Long?,
    val camp: Int,
    val noticeYn: Boolean,
    val fileYn: Boolean,
    val view: Int,
    val like: Int,
    val dislike: Int,
    val reply: Int,
    val title: String,
    val content: String?,
    val contentHtml: String?,
    val nickname: String?,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdDt: LocalDateTime,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedDt: LocalDateTime
) {
    companion object {
        fun of(entity: JunglebookPostEntity): JunglebookPostResponse {
            return JunglebookPostResponse(
                id = entity.id,
                tid = entity.tid,
                camp = entity.camp,
                noticeYn = entity.noticeYn,
                fileYn = entity.fileYn,
                view = entity.viewCnt,
                like = entity.likeCnt,
                dislike = entity.dislikeCnt,
                reply = entity.replyCnt,
                title = entity.title,
                content = entity.contentHtml,
                contentHtml = entity.contentHtml,
                nickname = entity.nickname,
                createdDt = entity.createdDt,
                updatedDt = entity.updatedDt
            )
        }
    }
}

data class JunglebookPostSimpleResponse(
    val id: Long?,
    val camp: Int,
    val view: Int,
    val like: Int,
    val reply: Int,
    val title: String,
    val nickname: String?,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdDt: LocalDateTime
) {
    companion object {
        fun of(entities: List<JunglebookPostEntity>?): List<JunglebookPostSimpleResponse> {
            return entities?.map { entity ->
                JunglebookPostSimpleResponse(
                    id = entity.id,
                    camp = entity.camp,
                    view = entity.viewCnt,
                    like = entity.likeCnt,
                    reply = entity.replyCnt,
                    title = entity.title,
                    nickname = entity.nickname,
                    createdDt = entity.createdDt
                )
            } ?: emptyList()
        }
    }
}

data class JunglebookResponse(
    val id: Long?,
    val title: String,
    val view: Int? = null,
    val like: Int? = null,
    val dislike: Int? = null,
    val post: Int,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val startDate: LocalDate?,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val endDate: LocalDate?,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdDt: LocalDateTime
) {
    companion object {
        fun of(entity: JunglebookEntity): JunglebookResponse {
            return JunglebookResponse(
                id = entity.id,
                title = entity.title,
                post = entity.postCnt,
                startDate = entity.startDate,
                endDate = entity.endDate,
                createdDt = entity.createdDt
            )
        }
    }
}