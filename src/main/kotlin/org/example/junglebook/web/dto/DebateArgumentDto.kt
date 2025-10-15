package org.example.junglebook.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import org.example.junglebook.entity.debate.DebateArgumentEntity
import org.example.junglebook.entity.debate.DebateTopicEntity
import org.example.junglebook.enums.ArgumentStance
import org.example.junglebook.enums.DebateTopicCategory
import java.time.LocalDate
import java.time.LocalDateTime


// 토론 주장 목록 응답
data class DebateArgumentListResponse(
    val totalCount: Int,
    val pageNo: Int,
    val arguments: List<DebateArgumentSimpleResponse>
) {
    companion object {
        fun of(totalCount: Int, pageNo: Int, entities: List<DebateArgumentEntity>?): DebateArgumentListResponse {
            return DebateArgumentListResponse(
                totalCount = totalCount,
                pageNo = pageNo,
                arguments = DebateArgumentSimpleResponse.of(entities)
            )
        }
    }
}

// 토론 주장 상세 응답
data class DebateArgumentResponse(
    val id: Long?,
    val topicId: Long,
    val authorId: Long,
    val authorNickname: String,
    val stance: ArgumentStance,
    val title: String,
    val content: String,
    val contentHtml: String?,
    val activeYn: Boolean,
    val noticeYn: Boolean,
    val fileYn: Boolean,
    val viewCount: Int,
    val supportCount: Int,
    val opposeCount: Int,
    val replyCount: Int,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: LocalDateTime,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun of(entity: DebateArgumentEntity): DebateArgumentResponse {
            return DebateArgumentResponse(
                id = entity.id,
                topicId = entity.topicId,
                authorId = entity.authorId,
                authorNickname = entity.authorNickname,
                stance = entity.stance,
                title = entity.title,
                content = entity.content,
                contentHtml = entity.contentHtml,
                activeYn = entity.activeYn,
                noticeYn = entity.noticeYn,
                fileYn = entity.fileYn,
                viewCount = entity.viewCount,
                supportCount = entity.supportCount,
                opposeCount = entity.opposeCount,
                replyCount = entity.replyCount,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
        }
    }
}

// 토론 주장 간단 응답 (목록용)
data class DebateArgumentSimpleResponse(
    val id: Long?,
    val stance: ArgumentStance,
    val title: String,
    val authorNickname: String,
    val viewCount: Int,
    val supportCount: Int,
    val opposeCount: Int,
    val replyCount: Int,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: LocalDateTime
) {
    companion object {
        fun of(entities: List<DebateArgumentEntity>?): List<DebateArgumentSimpleResponse> {
            return entities?.map { entity ->
                DebateArgumentSimpleResponse(
                    id = entity.id,
                    stance = entity.stance,
                    title = entity.title,
                    authorNickname = entity.authorNickname,
                    viewCount = entity.viewCount,
                    supportCount = entity.supportCount,
                    opposeCount = entity.opposeCount,
                    replyCount = entity.replyCount,
                    createdAt = entity.createdAt
                )
            } ?: emptyList()
        }
    }
}

// 입장별 통계 응답
data class DebateStanceStatisticsResponse(
    val topicId: Long,
    val forCount: Int,
    val againstCount: Int,
    val neutralCount: Int,
    val totalCount: Int
) {
    companion object {
        fun of(topicId: Long, statistics: Map<ArgumentStance, Int>): DebateStanceStatisticsResponse {
            val forCount = statistics[ArgumentStance.PRO] ?: 0
            val againstCount = statistics[ArgumentStance.CON] ?: 0
            val neutralCount = statistics[ArgumentStance.NEUTRAL] ?: 0

            return DebateStanceStatisticsResponse(
                topicId = topicId,
                forCount = forCount,
                againstCount = againstCount,
                neutralCount = neutralCount,
                totalCount = forCount + againstCount + neutralCount
            )
        }
    }
}


data class DebateArgumentCreateRequest(
    val stance: ArgumentStance,
    val title: String,
    val content: String,
    val contentHtml: String?,
    val authorNickname: String,
    val fileIds: List<Long>?
) {
    fun toEntity(topicId: Long, authorId: Long): DebateArgumentEntity {
        return DebateArgumentEntity(
            topicId = topicId,
            authorId = authorId,
            stance = stance,
            title = title,
            content = content,
            contentHtml = contentHtml,
            authorNickname = authorNickname,
            fileYn = !fileIds.isNullOrEmpty()
        )
    }
}

data class DebateArgumentUpdateRequest(
    val title: String,
    val content: String,
    val contentHtml: String?
)