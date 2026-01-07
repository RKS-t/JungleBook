package org.example.junglebook.service.debate

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.example.junglebook.entity.debate.DebateTopicEntity
import org.example.junglebook.enums.DebateTopicCategory
import org.example.junglebook.enums.DebateTopicStatus
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateTopicRepository
import org.example.junglebook.web.dto.DebateTopicCreateRequest
import org.example.junglebook.web.dto.DebateTopicUpdateRequest
import org.example.junglebook.web.dto.TopicSortType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class DebateTopicServiceUnitTest {

    @Mock
    private lateinit var debateTopicRepository: DebateTopicRepository

    @Mock
    private lateinit var debateArgumentRepository: DebateArgumentRepository

    @InjectMocks
    private lateinit var debateTopicService: DebateTopicService

    private lateinit var topicEntity: DebateTopicEntity

    @BeforeEach
    fun setUp() {
        topicEntity = DebateTopicEntity(
            id = 1L,
            creatorId = 100L,
            title = "테스트 토픽",
            description = "테스트 설명",
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
    fun `createTopic - 성공 케이스`() {
        val request = DebateTopicCreateRequest(
            title = "새 토픽",
            description = "새 설명",
            descriptionHtml = null,
            category = DebateTopicCategory.POLITICS,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(30)
        )
        val creatorId = 100L

        whenever(debateTopicRepository.save(any())).thenReturn(topicEntity.copy(
            title = request.title,
            description = request.description
        ))

        val result = debateTopicService.createTopic(request, creatorId)

        assertThat(result).isNotNull
        assertThat(result.title).isEqualTo(request.title)
        verify(debateTopicRepository).save(any())
    }

    @Test
    fun `getTopicDetail - 성공 케이스`() {
        val topicId = 1L

        whenever(debateTopicRepository.findByIdAndActiveYnTrue(topicId)).thenReturn(topicEntity)
        whenever(debateTopicRepository.increaseViewCount(topicId)).thenReturn(Unit)
        whenever(debateArgumentRepository.countByTopicIdGroupByStance(topicId)).thenReturn(emptyList())
        whenever(debateArgumentRepository.countByTopicIdAndActiveYnTrueAndCreatedAtBetween(any(), any(), any())).thenReturn(0L)
        whenever(debateArgumentRepository.findPopularByStance(any(), any())).thenReturn(emptyList())

        val result = debateTopicService.getTopicDetail(topicId, increaseView = true)

        assertThat(result).isNotNull
        assertThat(result?.topic?.id).isEqualTo(topicId)
        verify(debateTopicRepository).increaseViewCount(topicId)
    }

    @Test
    fun `getTopicDetail - 토픽을 찾을 수 없는 경우`() {
        val topicId = 999L

        whenever(debateTopicRepository.findByIdAndActiveYnTrue(topicId)).thenReturn(null)

        val result = debateTopicService.getTopicDetail(topicId)

        assertThat(result).isNull()
        verify(debateTopicRepository, never()).increaseViewCount(any())
    }

    @Test
    fun `updateTopic - 성공 케이스`() {
        val topicId = 1L
        val userId = 100L
        val request = DebateTopicUpdateRequest(
            title = "수정된 제목",
            description = "수정된 설명",
            descriptionHtml = null,
            category = null,
            status = null,
            hotYn = null,
            startDate = null,
            endDate = null
        )

        whenever(debateTopicRepository.findByIdAndActiveYnTrue(topicId)).thenReturn(topicEntity)
        whenever(debateTopicRepository.save(any())).thenReturn(topicEntity.copy(
            title = request.title!!,
            description = request.description!!
        ))

        val result = debateTopicService.updateTopic(topicId, request, userId)

        assertThat(result).isNotNull
        assertThat(result?.title).isEqualTo(request.title)
        verify(debateTopicRepository).save(any())
    }

    @Test
    fun `updateTopic - 작성자가 아닌 경우 예외 발생`() {
        val topicId = 1L
        val otherUserId = 999L
        val request = DebateTopicUpdateRequest(
            title = "수정된 제목",
            description = null,
            descriptionHtml = null,
            category = null,
            status = null,
            hotYn = null,
            startDate = null,
            endDate = null
        )

        whenever(debateTopicRepository.findByIdAndActiveYnTrue(topicId)).thenReturn(topicEntity)

        assertThatThrownBy {
            debateTopicService.updateTopic(topicId, request, otherUserId)
        }.isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.DEBATE_TOPIC_MODIFY_DENIED
            }
    }

    @Test
    fun `deleteTopic - 성공 케이스`() {
        val topicId = 1L
        val userId = 100L

        whenever(debateTopicRepository.findByIdAndActiveYnTrue(topicId)).thenReturn(topicEntity)
        whenever(debateTopicRepository.save(any())).thenReturn(topicEntity.copy(activeYn = false))

        debateTopicService.deleteTopic(topicId, userId)

        verify(debateTopicRepository).save(any())
    }

    @Test
    fun `deleteTopic - 작성자가 아닌 경우 예외 발생`() {
        val topicId = 1L
        val otherUserId = 999L

        whenever(debateTopicRepository.findByIdAndActiveYnTrue(topicId)).thenReturn(topicEntity)

        assertThatThrownBy {
            debateTopicService.deleteTopic(topicId, otherUserId)
        }.isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.DEBATE_TOPIC_DELETE_DENIED
            }
    }

    @Test
    fun `getTopicList - 성공 케이스`() {
        val sortType = TopicSortType.LATEST
        val pageNo = 0
        val limit = 20

        whenever(debateTopicRepository.findByActiveYnTrueOrderByCreatedAtDesc(any()))
            .thenReturn(PageImpl(listOf(topicEntity)))

        val result = debateTopicService.getTopicList(sortType, pageNo, limit)

        assertThat(result).isNotNull
        assertThat(result.totalCount).isEqualTo(1)
    }
}

