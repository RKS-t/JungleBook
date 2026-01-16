package org.example.junglebook.service.debate

import org.example.junglebook.constant.JBConstants
import org.example.junglebook.enums.ArgumentStance
import org.example.junglebook.enums.DebateTopicCategory
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateTopicRepository
import org.example.junglebook.web.dto.DebateArgumentSimpleResponse
import org.example.junglebook.web.dto.DebateTopicDashboardResponse
import org.example.junglebook.web.dto.DebateTopicDetailResponse
import org.example.junglebook.web.dto.DebateTopicListResponse
import org.example.junglebook.web.dto.DebateTopicResponse
import org.example.junglebook.web.dto.DebateTopicSearchRequest
import org.example.junglebook.web.dto.DebateTopicSimpleResponse
import org.example.junglebook.web.dto.TopicSortType
import org.example.junglebook.web.dto.TopicStatistics
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class DebateTopicQueryService(
    private val debateTopicRepository: DebateTopicRepository,
    private val debateArgumentRepository: DebateArgumentRepository
) {

    @Transactional(readOnly = true)
    fun getTopicDetail(topicId: Long): DebateTopicDetailResponse? {
        val topic = debateTopicRepository.findByIdAndActiveYnTrue(topicId) ?: return null

        val stanceDistribution = debateArgumentRepository.countByTopicIdGroupByStance(topicId)
            .associate { (it[0] as ArgumentStance) to (it[1] as Long).toInt() }

        val weekAgo = LocalDateTime.now().minusDays(JBConstants.DEBATE_RECENT_WEEKS.toLong())
        val recentCount = debateArgumentRepository.countByTopicIdAndActiveYnTrueAndCreatedAtBetween(
            topicId, weekAgo, LocalDateTime.now()
        ).toInt()

        val daysSinceCreation = ChronoUnit.DAYS.between(topic.createdAt.toLocalDate(), LocalDate.now()) + 1
        val avgPerDay = if (daysSinceCreation > 0) topic.argumentCount.toDouble() / daysSinceCreation else 0.0

        val topArguments = ArgumentStance.values().associateWith { stance ->
            val arguments = debateArgumentRepository.findPopularByStance(topicId, stance)
            DebateArgumentSimpleResponse.of(arguments.take(JBConstants.DEBATE_TOP_ARGUMENTS_LIMIT))
        }

        val statistics = TopicStatistics(
            totalArguments = topic.argumentCount,
            stanceDistribution = stanceDistribution,
            totalViews = topic.viewCount,
            recentWeeklyArguments = recentCount,
            averageArgumentsPerDay = avgPerDay
        )

        return DebateTopicDetailResponse(
            topic = DebateTopicResponse.of(topic, stanceDistribution),
            statistics = statistics,
            topArguments = topArguments
        )
    }

    @Transactional(readOnly = true)
    fun getTopicList(sortType: TopicSortType, pageNo: Int, limit: Int): DebateTopicListResponse {
        val pageable = PageRequest.of(pageNo, limit)

        val page = when (sortType) {
            TopicSortType.LATEST ->
                debateTopicRepository.findByActiveYnTrueOrderByCreatedAtDesc(pageable)
            TopicSortType.POPULAR ->
                debateTopicRepository.findByActiveYnTrueOrderByPopularity(pageable)
            TopicSortType.MOST_VIEWED ->
                debateTopicRepository.findByActiveYnTrueOrderByViewCountDesc(pageable)
            TopicSortType.MOST_ARGUED ->
                debateTopicRepository.findByActiveYnTrueOrderByArgumentCountDesc(pageable)
            TopicSortType.ENDING_SOON ->
                debateTopicRepository.findEndingSoonTopics(LocalDate.now(), pageable)
        }

        return DebateTopicListResponse.of(
            page.totalElements.toInt(),
            pageNo,
            limit,
            page.content
        )
    }

    @Transactional(readOnly = true)
    fun getHotTopics(limit: Int = JBConstants.DEBATE_DEFAULT_HOT_TOPICS_LIMIT): List<DebateTopicSimpleResponse> {
        val pageable = PageRequest.of(0, limit)
        val topics = debateTopicRepository.findByHotYnTrueAndActiveYnTrueOrderByViewCountDesc(pageable)
        return DebateTopicSimpleResponse.of(topics.content)
    }

    @Transactional(readOnly = true)
    fun getTopicsByCategory(
        category: DebateTopicCategory,
        pageNo: Int,
        limit: Int
    ): DebateTopicListResponse {
        val pageable = PageRequest.of(pageNo, limit)
        val page = debateTopicRepository.findByCategoryAndActiveYnTrueOrderByCreatedAtDesc(category, pageable)

        return DebateTopicListResponse.of(
            page.totalElements.toInt(),
            pageNo,
            limit,
            page.content
        )
    }

    @Transactional(readOnly = true)
    fun searchTopics(request: DebateTopicSearchRequest): DebateTopicListResponse {
        val pageable = PageRequest.of(request.pageNo, request.limit)

        val page = if (request.keyword.isNullOrBlank() &&
            request.category == null &&
            request.status == null &&
            !request.hotOnly) {
            return getTopicList(request.sortType, request.pageNo, request.limit)
        } else {
            debateTopicRepository.searchWithFilters(
                request.category,
                request.status,
                request.keyword?.trim(),
                request.hotOnly,
                pageable
            )
        }

        return DebateTopicListResponse.of(
            page.totalElements.toInt(),
            request.pageNo,
            request.limit,
            page.content
        )
    }

    @Transactional(readOnly = true)
    fun getOngoingTopics(pageNo: Int, limit: Int): DebateTopicListResponse {
        val pageable = PageRequest.of(pageNo, limit)
        val page = debateTopicRepository.findOngoingTopics(LocalDate.now(), pageable)

        return DebateTopicListResponse.of(
            page.totalElements.toInt(),
            pageNo,
            limit,
            page.content
        )
    }

    @Transactional(readOnly = true)
    fun getEndingSoonTopics(limit: Int = JBConstants.DEBATE_DEFAULT_ENDING_SOON_LIMIT): List<DebateTopicSimpleResponse> {
        val pageable = PageRequest.of(0, limit)
        val topics = debateTopicRepository.findEndingSoonTopics(LocalDate.now(), pageable)
        return DebateTopicSimpleResponse.of(topics.content)
    }

    @Transactional(readOnly = true)
    fun getDashboard(): DebateTopicDashboardResponse {
        val hotTopics = getHotTopics(JBConstants.DEBATE_DASHBOARD_TOPICS_LIMIT)
        val newTopics = getTopicList(TopicSortType.LATEST, 0, JBConstants.DEBATE_DASHBOARD_TOPICS_LIMIT).topics
        val endingSoonTopics = getEndingSoonTopics(JBConstants.DEBATE_DASHBOARD_TOPICS_LIMIT)

        val categoryDistribution = debateTopicRepository.countByCategory()
            .associate {
                (it[0] as DebateTopicCategory) to (it[1] as Long).toInt()
            }

        return DebateTopicDashboardResponse(
            hotTopics = hotTopics,
            newTopics = newTopics,
            endingSoonTopics = endingSoonTopics,
            categoryDistribution = categoryDistribution
        )
    }

    @Transactional(readOnly = true)
    fun getTopicStatistics(topicId: Long): TopicStatistics? {
        val topic = debateTopicRepository.findByIdAndActiveYnTrue(topicId) ?: return null

        val stanceDistribution = debateArgumentRepository.countByTopicIdGroupByStance(topicId)
            .associate { (it[0] as ArgumentStance) to (it[1] as Long).toInt() }

        val weekAgo = LocalDateTime.now().minusDays(JBConstants.DEBATE_RECENT_WEEKS.toLong())
        val recentCount = debateArgumentRepository.countByTopicIdAndActiveYnTrueAndCreatedAtBetween(
            topicId, weekAgo, LocalDateTime.now()
        ).toInt()

        val daysSinceCreation = ChronoUnit.DAYS.between(topic.createdAt.toLocalDate(), LocalDate.now()) + 1
        val avgPerDay = if (daysSinceCreation > 0) topic.argumentCount.toDouble() / daysSinceCreation else 0.0

        return TopicStatistics(
            totalArguments = topic.argumentCount,
            stanceDistribution = stanceDistribution,
            totalViews = topic.viewCount,
            recentWeeklyArguments = recentCount,
            averageArgumentsPerDay = avgPerDay
        )
    }
}
