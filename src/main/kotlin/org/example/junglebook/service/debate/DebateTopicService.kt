package org.example.junglebook.service.debate


import org.example.junglebook.enums.ArgumentStance
import org.example.junglebook.enums.DebateTopicCategory
import org.example.junglebook.enums.DebateTopicStatus
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateTopicRepository
import org.example.junglebook.web.dto.DebateArgumentSimpleResponse
import org.example.junglebook.web.dto.DebateTopicCreateRequest
import org.example.junglebook.web.dto.DebateTopicDashboardResponse
import org.example.junglebook.web.dto.DebateTopicDetailResponse
import org.example.junglebook.web.dto.DebateTopicListResponse
import org.example.junglebook.web.dto.DebateTopicResponse
import org.example.junglebook.web.dto.DebateTopicSearchRequest
import org.example.junglebook.web.dto.DebateTopicSimpleResponse
import org.example.junglebook.web.dto.DebateTopicUpdateRequest
import org.example.junglebook.web.dto.TopicSortType
import org.example.junglebook.web.dto.TopicStatistics
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit


@Service
class DebateTopicService(
    private val debateTopicRepository: DebateTopicRepository,
    private val debateArgumentRepository: DebateArgumentRepository,
) {

    /**
     * 1. 토픽 생성
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun createTopic(request: DebateTopicCreateRequest, creatorId: Long): DebateTopicResponse {
        val entity = request.toEntity(creatorId)
        val saved = debateTopicRepository.save(entity)
        return DebateTopicResponse.of(saved)
    }

    /**
     * 2. 토픽 상세 조회
     */
    @Transactional(readOnly = true)
    fun getTopicDetail(topicId: Long, increaseView: Boolean = true): DebateTopicDetailResponse? {
        val topic = debateTopicRepository.findByIdAndActiveYnTrue(topicId) ?: return null

        // 조회수 증가 (비동기나 별도 처리 권장)
        if (increaseView) {
            debateTopicRepository.increaseViewCount(topicId)
        }

        // 입장별 통계
        val stanceDistribution = debateArgumentRepository.countByTopicIdGroupByStance(topicId)
            .associate { (it[0] as ArgumentStance) to (it[1] as Long).toInt() }

        // 최근 7일간 논증 수
        val weekAgo = LocalDateTime.now().minusDays(7)
        val recentCount = debateArgumentRepository.countByTopicIdAndActiveYnTrueAndCreatedAtBetween(
            topicId, weekAgo, LocalDateTime.now()
        ).toInt()

        // 일평균 논증 수
        val daysSinceCreation = ChronoUnit.DAYS.between(topic.createdAt.toLocalDate(), LocalDate.now()) + 1
        val avgPerDay = if (daysSinceCreation > 0) topic.argumentCount.toDouble() / daysSinceCreation else 0.0

        // 입장별 인기 논증 (각 입장당 상위 3개)
        val topArguments = ArgumentStance.values().associateWith { stance ->
            val arguments = debateArgumentRepository.findPopularByStance(topicId, stance)
            DebateArgumentSimpleResponse.of(arguments.take(3))
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

    /**
     * 3. 토픽 목록 조회 (정렬 기준별)
     */
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

    /**
     * 4. Hot 토픽 조회
     */
    @Transactional(readOnly = true)
    fun getHotTopics(limit: Int = 10): List<DebateTopicSimpleResponse> {
        val pageable = PageRequest.of(0, limit)
        val topics = debateTopicRepository.findByHotYnTrueAndActiveYnTrueOrderByViewCountDesc(pageable)
        return DebateTopicSimpleResponse.of(topics.content)
    }

    /**
     * 5. 카테고리별 토픽 조회
     */
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

    /**
     * 6. 토픽 검색
     */
    @Transactional(readOnly = true)
    fun searchTopics(request: DebateTopicSearchRequest): DebateTopicListResponse {
        val pageable = PageRequest.of(request.pageNo, request.limit)

        val page = if (request.keyword.isNullOrBlank() &&
            request.category == null &&
            request.status == null &&
            !request.hotOnly) {
            // 필터 없으면 정렬만 적용
            getTopicList(request.sortType, request.pageNo, request.limit)
            return getTopicList(request.sortType, request.pageNo, request.limit)
        } else {
            // 복합 필터 적용
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

    /**
     * 7. 진행 중인 토픽
     */
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

    /**
     * 8. 마감 임박 토픽
     */
    @Transactional(readOnly = true)
    fun getEndingSoonTopics(limit: Int = 10): List<DebateTopicSimpleResponse> {
        val pageable = PageRequest.of(0, limit)
        val topics = debateTopicRepository.findEndingSoonTopics(LocalDate.now(), pageable)
        return DebateTopicSimpleResponse.of(topics.content)
    }


    /**
     * 12. 토픽 수정
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun updateTopic(topicId: Long, request: DebateTopicUpdateRequest, userId: Long): DebateTopicResponse? {
        val topic = debateTopicRepository.findByIdAndActiveYnTrue(topicId) ?: return null

        // 권한 검증 (작성자만 수정 가능)
        if (topic.creatorId != userId) {
            throw IllegalAccessException("토픽 수정 권한이 없습니다.")
        }

        // 업데이트
        val updated = topic.copy(
            title = request.title ?: topic.title,
            description = request.description ?: topic.description,
            descriptionHtml = request.descriptionHtml ?: topic.descriptionHtml,
            category = request.category ?: topic.category,
            status = request.status ?: topic.status,
            hotYn = request.hotYn ?: topic.hotYn,
            startDate = request.startDate ?: topic.startDate,
            endDate = request.endDate ?: topic.endDate,
            updatedAt = LocalDateTime.now()
        )

        val saved = debateTopicRepository.save(updated)
        return DebateTopicResponse.of(saved)
    }

    /**
     * 13. 토픽 삭제 (Soft Delete)
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun deleteTopic(topicId: Long, userId: Long): Boolean {
        val topic = debateTopicRepository.findByIdAndActiveYnTrue(topicId) ?: return false

        // 권한 검증
        if (topic.creatorId != userId) {
            throw IllegalAccessException("토픽 삭제 권한이 없습니다.")
        }

        val deleted = topic.copy(activeYn = false, updatedAt = LocalDateTime.now())
        debateTopicRepository.save(deleted)
        return true
    }

    /**
     * 14. 대시보드 데이터
     */
    @Transactional(readOnly = true)
    fun getDashboard(): DebateTopicDashboardResponse {
        val hotTopics = getHotTopics(5)
        val newTopics = getTopicList(TopicSortType.LATEST, 0, 5).topics
        val endingSoonTopics = getEndingSoonTopics(5)

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

    /**
     * 15. Hot 토픽 자동 설정 (배치나 스케줄러에서 호출)
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun updateHotTopics(threshold: Int = 100) {
        // 모든 활성 토픽 조회
        val allTopics = debateTopicRepository.findAll()
            .filter { it.activeYn }

        allTopics.forEach { topic ->
            val score = topic.argumentCount + (topic.viewCount / 10) // 점수 계산
            val shouldBeHot = score >= threshold

            if (topic.hotYn != shouldBeHot) {
                val updated = topic.copy(
                    hotYn = shouldBeHot,
                    updatedAt = LocalDateTime.now()
                )
                debateTopicRepository.save(updated)
            }
        }
    }

    /**
     * 16. 토픽 통계 조회
     */
    @Transactional(readOnly = true)
    fun getTopicStatistics(topicId: Long): TopicStatistics? {
        val topic = debateTopicRepository.findByIdAndActiveYnTrue(topicId) ?: return null

        val stanceDistribution = debateArgumentRepository.countByTopicIdGroupByStance(topicId)
            .associate { (it[0] as ArgumentStance) to (it[1] as Long).toInt() }

        val weekAgo = LocalDateTime.now().minusDays(7)
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

    /**
     * 17. 토픽 상태 변경
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun changeTopicStatus(topicId: Long, status: DebateTopicStatus, userId: Long): DebateTopicResponse? {
        val topic = debateTopicRepository.findByIdAndActiveYnTrue(topicId) ?: return null

        if (topic.creatorId != userId) {
            throw IllegalAccessException("토픽 상태 변경 권한이 없습니다.")
        }

        val updated = topic.copy(
            status = status,
            updatedAt = LocalDateTime.now()
        )

        val saved = debateTopicRepository.save(updated)
        return DebateTopicResponse.of(saved)
    }

    /**
     * 18. 논증 생성 시 토픽 카운트 증가
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun increaseArgumentCount(topicId: Long) {
        debateTopicRepository.increaseArgumentCount(topicId)
    }

    /**
     * 19. 논증 삭제 시 토픽 카운트 감소
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun decreaseArgumentCount(topicId: Long) {
        debateTopicRepository.decreaseArgumentCount(topicId)
    }
}