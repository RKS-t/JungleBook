package org.example.junglebook.web.dto

import org.example.junglebook.entity.debate.DebateTopicEntity
import org.example.junglebook.enums.DebateTopicCategory
import org.example.junglebook.enums.DebateTopicStatus
import org.example.junglebook.enums.ArgumentStance
import java.time.LocalDate
import java.time.LocalDateTime

// ===== Request DTOs =====

data class DebateTopicCreateRequest(
    val title: String,
    val description: String,
    val descriptionHtml: String?,
    val category: DebateTopicCategory,
    val startDate: LocalDate?,
    val endDate: LocalDate?
) {
    fun toEntity(creatorId: Long): DebateTopicEntity {
        return DebateTopicEntity(
            title = title,
            description = description,
            descriptionHtml = descriptionHtml,
            category = category,
            status = DebateTopicStatus.DEBATING,
            creatorId = creatorId,
            startDate = startDate,
            endDate = endDate
        )
    }
}

data class DebateTopicUpdateRequest(
    val title: String?,
    val description: String?,
    val descriptionHtml: String?,
    val category: DebateTopicCategory?,
    val status: DebateTopicStatus?,
    val hotYn: Boolean?,
    val startDate: LocalDate?,
    val endDate: LocalDate?
)

data class DebateTopicSearchRequest(
    val keyword: String? = null,
    val category: DebateTopicCategory? = null,
    val status: DebateTopicStatus? = null,
    val hotOnly: Boolean = false,
    val sortType: TopicSortType = TopicSortType.LATEST,
    val pageNo: Int = 0,
    val limit: Int = 20
)

enum class TopicSortType {
    LATEST,           // 최신순
    POPULAR,          // 인기순 (논증 수 + 조회수)
    MOST_VIEWED,      // 조회수순
    MOST_ARGUED,      // 논증 많은 순
    ENDING_SOON       // 마감 임박순
}

// ===== Response DTOs =====

data class DebateTopicResponse(
    val id: Long,
    val title: String,
    val description: String,
    val descriptionHtml: String?,
    val category: DebateTopicCategory,
    val status: DebateTopicStatus,
    val creatorId: Long?,
    val hotYn: Boolean,
    val activeYn: Boolean,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val argumentCount: Int,
    val viewCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,

    // 추가 정보
    val isActive: Boolean,
    val daysRemaining: Long?,
    val stanceDistribution: Map<ArgumentStance, Int>? = null,
    val recentArguments: List<DebateArgumentSimpleResponse>? = null
) {
    companion object {
        fun of(
            entity: DebateTopicEntity,
            stanceDistribution: Map<ArgumentStance, Int>? = null,
            recentArguments: List<DebateArgumentSimpleResponse>? = null
        ): DebateTopicResponse {
            val now = LocalDate.now()
            val daysRemaining = entity.endDate?.let {
                java.time.temporal.ChronoUnit.DAYS.between(now, it)
            }

            return DebateTopicResponse(
                id = entity.id!!,
                title = entity.title,
                description = entity.description,
                descriptionHtml = entity.descriptionHtml!!,
                category = entity.category,
                status = entity.status,
                creatorId = entity.creatorId,
                hotYn = entity.hotYn,
                activeYn = entity.activeYn,
                startDate = entity.startDate,
                endDate = entity.endDate,
                argumentCount = entity.argumentCount,
                viewCount = entity.viewCount,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                isActive = entity.status == DebateTopicStatus.DEBATING &&
                        (entity.endDate == null || entity.endDate!! >= now),
                daysRemaining = if (daysRemaining != null && daysRemaining >= 0) daysRemaining else null,
                stanceDistribution = stanceDistribution,
                recentArguments = recentArguments
            )
        }
    }
}

data class DebateTopicSimpleResponse(
    val id: Long,
    val title: String,
    val category: DebateTopicCategory,
    val status: DebateTopicStatus,
    val hotYn: Boolean,
    val argumentCount: Int,
    val viewCount: Int,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val daysRemaining: Long?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun of(entity: DebateTopicEntity): DebateTopicSimpleResponse {
            val now = LocalDate.now()
            val daysRemaining = entity.endDate?.let {
                java.time.temporal.ChronoUnit.DAYS.between(now, it)
            }

            return DebateTopicSimpleResponse(
                id = entity.id!!,
                title = entity.title,
                category = entity.category,
                status = entity.status,
                hotYn = entity.hotYn,
                argumentCount = entity.argumentCount,
                viewCount = entity.viewCount,
                startDate = entity.startDate,
                endDate = entity.endDate,
                daysRemaining = if (daysRemaining != null && daysRemaining >= 0) daysRemaining else null,
                createdAt = entity.createdAt
            )
        }

        fun of(entities: List<DebateTopicEntity>): List<DebateTopicSimpleResponse> {
            return entities.map { of(it) }
        }
    }
}

data class DebateTopicListResponse(
    val totalCount: Int,
    val currentPage: Int,
    val totalPages: Int,
    val topics: List<DebateTopicSimpleResponse>
) {
    companion object {
        fun of(totalCount: Int, pageNo: Int, limit: Int, topics: List<DebateTopicEntity>): DebateTopicListResponse {
            val totalPages = if (totalCount == 0) 0 else (totalCount + limit - 1) / limit

            return DebateTopicListResponse(
                totalCount = totalCount,
                currentPage = pageNo,
                totalPages = totalPages,
                topics = DebateTopicSimpleResponse.of(topics)
            )
        }
    }
}

data class DebateTopicDetailResponse(
    val topic: DebateTopicResponse,
    val statistics: TopicStatistics,
    val topArguments: Map<ArgumentStance, List<DebateArgumentSimpleResponse>>
)

data class TopicStatistics(
    val totalArguments: Int,
    val stanceDistribution: Map<ArgumentStance, Int>,
    val totalViews: Int,
    val recentWeeklyArguments: Int,
    val averageArgumentsPerDay: Double
)

data class DebateTopicDashboardResponse(
    val hotTopics: List<DebateTopicSimpleResponse>,
    val newTopics: List<DebateTopicSimpleResponse>,
    val endingSoonTopics: List<DebateTopicSimpleResponse>,
    val categoryDistribution: Map<DebateTopicCategory, Int>
)