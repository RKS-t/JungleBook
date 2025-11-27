package org.example.junglebook.web.dto

import org.example.junglebook.entity.debate.DebateReplyEntity

data class DebateReplyCreateRequest(
    val parentId: Long?,
    val content: String,
    val contentHtml: String?,
    val authorNickname: String,
    val fileIds: List<Long>?
) {
    fun toEntity(argumentId: Long, userId: Long): DebateReplyEntity {
        // 부모 댓글 ID가 있으면 대댓글(depth=1), 없으면 최상위 댓글(depth=0)
        val depth = if (parentId != null) 1 else 0

        return DebateReplyEntity(
            argumentId = argumentId,
            userId = userId,
            parentId = parentId,
            depth = depth,
            content = content,
            contentHtml = contentHtml,
            authorNickname = authorNickname,
            fileYn = !fileIds.isNullOrEmpty()
        )
    }
}

// 토론 댓글은 수정 불가 (토론의 무결성을 위해)


data class DebateReplyResponse(
    val id: Long,
    val argumentId: Long,
    val userId: Long,
    val parentId: Long?,
    val depth: Int,
    val content: String,
    val contentHtml: String?,
    val authorNickname: String,
    val activeYn: Boolean,
    val fileYn: Boolean,
    val supportCount: Int,
    val opposeCount: Int,
    val createdAt: java.time.LocalDateTime,
    val updatedAt: java.time.LocalDateTime,
    val childCount: Int = 0
) {
    companion object {
        fun of(entity: DebateReplyEntity, childCount: Int = 0): DebateReplyResponse {
            return DebateReplyResponse(
                id = entity.id!!,
                argumentId = entity.argumentId,
                userId = entity.userId,
                parentId = entity.parentId,
                depth = entity.depth,
                content = entity.content,
                contentHtml = entity.contentHtml,
                authorNickname = entity.authorNickname,
                activeYn = entity.activeYn,
                fileYn = entity.fileYn,
                supportCount = entity.supportCount,
                opposeCount = entity.opposeCount,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                childCount = childCount
            )
        }
    }
}

data class DebateReplySimpleResponse(
    val id: Long,
    val content: String,
    val authorNickname: String,
    val supportCount: Int,
    val opposeCount: Int,
    val createdAt: java.time.LocalDateTime
) {
    companion object {
        fun of(entity: DebateReplyEntity): DebateReplySimpleResponse {
            return DebateReplySimpleResponse(
                id = entity.id!!,
                content = entity.content,
                authorNickname = entity.authorNickname,
                supportCount = entity.supportCount,
                opposeCount = entity.opposeCount,
                createdAt = entity.createdAt
            )
        }

        fun of(entities: List<DebateReplyEntity>): List<DebateReplySimpleResponse> {
            return entities.map { of(it) }
        }
    }
}

data class DebateReplyListResponse(
    val totalCount: Int,
    val currentPage: Int,
    val totalPages: Int,
    val replies: List<DebateReplyResponse>
) {
    companion object {
        fun of(totalCount: Int, pageNo: Int, replies: List<DebateReplyEntity>): DebateReplyListResponse {
            val limit = 20
            val totalPages = if (totalCount == 0) 0 else (totalCount + limit - 1) / limit

            return DebateReplyListResponse(
                totalCount = totalCount,
                currentPage = pageNo,
                totalPages = totalPages,
                replies = replies.map { DebateReplyResponse.of(it) }
            )
        }
    }
}

data class ReplyStatistics(
    val totalReplies: Int,
    val recentWeeklyReplies: Int
)