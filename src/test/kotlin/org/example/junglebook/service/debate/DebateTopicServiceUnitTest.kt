package org.example.junglebook.service.debate

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.example.junglebook.entity.debate.DebateTopicEntity
import org.example.junglebook.enums.DebateTopicCategory
import org.example.junglebook.enums.DebateTopicStatus
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.repository.debate.DebateTopicRepository
import org.example.junglebook.web.dto.DebateTopicCreateRequest
import org.example.junglebook.web.dto.DebateTopicUpdateRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.ArgumentMatchers.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class DebateTopicCommandServiceUnitTest {

    @Mock
    private lateinit var debateTopicRepository: DebateTopicRepository

    @InjectMocks
    private lateinit var debateTopicCommandService: DebateTopicCommandService

    private val topicEntity = DebateTopicEntity(
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

        doReturn(topicEntity.copy(
            title = request.title,
            description = request.description
        )).whenever(debateTopicRepository).save(any(DebateTopicEntity::class.java))

        val result = debateTopicCommandService.createTopic(request, creatorId)

        assertThat(result).isNotNull
        assertThat(result.title).isEqualTo(request.title)
        verify(debateTopicRepository).save(any(DebateTopicEntity::class.java))
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
        doReturn(topicEntity.copy(
            title = request.title!!,
            description = request.description!!
        )).whenever(debateTopicRepository).save(any(DebateTopicEntity::class.java))

        val result = debateTopicCommandService.updateTopic(topicId, request, userId)

        assertThat(result).isNotNull
        assertThat(result?.title).isEqualTo(request.title)
        verify(debateTopicRepository).save(any(DebateTopicEntity::class.java))
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
            debateTopicCommandService.updateTopic(topicId, request, otherUserId)
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
        doReturn(topicEntity.copy(activeYn = false)).whenever(debateTopicRepository).save(any(DebateTopicEntity::class.java))

        debateTopicCommandService.deleteTopic(topicId, userId)

        verify(debateTopicRepository).save(any(DebateTopicEntity::class.java))
    }

    @Test
    fun `deleteTopic - 작성자가 아닌 경우 예외 발생`() {
        val topicId = 1L
        val otherUserId = 999L

        whenever(debateTopicRepository.findByIdAndActiveYnTrue(topicId)).thenReturn(topicEntity)

        assertThatThrownBy {
            debateTopicCommandService.deleteTopic(topicId, otherUserId)
        }.isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.DEBATE_TOPIC_DELETE_DENIED
            }
    }
}

