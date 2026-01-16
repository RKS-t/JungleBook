package org.example.junglebook.service.debate

import org.assertj.core.api.Assertions.assertThat
import org.example.junglebook.entity.debate.DebateTopicEntity
import org.example.junglebook.enums.ArgumentStance
import org.example.junglebook.enums.DebateTopicCategory
import org.example.junglebook.enums.DebateTopicStatus
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateTopicRepository
import org.example.junglebook.web.dto.TopicSortType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class DebateTopicQueryServiceUnitTest {

    @Mock
    private lateinit var debateTopicRepository: DebateTopicRepository

    @Mock
    private lateinit var debateArgumentRepository: DebateArgumentRepository

    @InjectMocks
    private lateinit var debateTopicQueryService: DebateTopicQueryService

    private lateinit var topicEntity: DebateTopicEntity

    @BeforeEach
    fun setUp() {
        topicEntity = DebateTopicEntity(
            id = 1L,
            creatorId = 100L,
            title = "테스트 토픽",
            description = "테스트 설명",
            descriptionHtml = "",
            category = DebateTopicCategory.POLITICS,
            status = DebateTopicStatus.DEBATING,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(30),
            activeYn = true,
            hotYn = false,
            argumentCount = 0,
            viewCount = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `getTopicDetail - 성공 케이스`() {
        val topicId = 1L

        whenever(debateTopicRepository.findByIdAndActiveYnTrue(topicId)).thenReturn(topicEntity)
        whenever(debateArgumentRepository.countByTopicIdGroupByStance(topicId)).thenReturn(emptyList())
        ArgumentStance.values().forEach { stance ->
            whenever(debateArgumentRepository.findPopularByStance(topicId, stance)).thenReturn(emptyList())
        }

        val result = debateTopicQueryService.getTopicDetail(topicId)

        assertThat(result).isNotNull
        assertThat(result?.topic?.id).isEqualTo(topicId)
        verify(debateTopicRepository, never()).increaseViewCount(anyLong())
    }

    @Test
    fun `getTopicDetail - 토픽을 찾을 수 없는 경우`() {
        val topicId = 999L

        whenever(debateTopicRepository.findByIdAndActiveYnTrue(topicId)).thenReturn(null)

        val result = debateTopicQueryService.getTopicDetail(topicId)

        assertThat(result).isNull()
    }

    @Test
    fun `getTopicList - 성공 케이스`() {
        val sortType = TopicSortType.LATEST
        val pageNo = 0
        val limit = 20

        whenever(debateTopicRepository.findByActiveYnTrueOrderByCreatedAtDesc(PageRequest.of(pageNo, limit)))
            .thenReturn(PageImpl(listOf(topicEntity)))

        val result = debateTopicQueryService.getTopicList(sortType, pageNo, limit)

        assertThat(result).isNotNull
        assertThat(result.totalCount).isEqualTo(1)
    }
}
