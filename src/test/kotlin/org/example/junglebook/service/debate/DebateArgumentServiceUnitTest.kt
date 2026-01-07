package org.example.junglebook.service.debate

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.example.junglebook.entity.debate.DebateArgumentEntity
import org.example.junglebook.enums.ArgumentStance
import org.example.junglebook.enums.DebateReferenceType
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateFileRepository
import org.example.junglebook.service.fallacy.FallacyDetectionService
import org.example.junglebook.web.dto.DebateArgumentResponse
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
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@ExtendWith(MockitoExtension::class)
class DebateArgumentServiceUnitTest {

    @Mock
    private lateinit var debateArgumentRepository: DebateArgumentRepository

    @Mock
    private lateinit var debateFileRepository: DebateFileRepository

    @Mock
    private lateinit var debateTopicService: DebateTopicService

    @Mock
    private lateinit var fallacyDetectionService: FallacyDetectionService

    @InjectMocks
    private lateinit var debateArgumentService: DebateArgumentService

    private lateinit var argumentEntity: DebateArgumentEntity

    @BeforeEach
    fun setUp() {
        argumentEntity = DebateArgumentEntity(
            id = 1L,
            topicId = 1L,
            userId = 100L,
            stance = ArgumentStance.PRO,
            title = "테스트 논증",
            content = "테스트 내용",
            authorNickname = "테스트유저",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `createArgument - 성공 케이스`() {
        val fileIds = listOf(1L, 2L)
        val savedEntity = argumentEntity.copy(id = 1L)

        whenever(debateArgumentRepository.save(any())).thenReturn(savedEntity)
        whenever(debateFileRepository.updateAttachStatus(any(), any(), any(), any())).thenReturn(1)
        whenever(debateTopicService.increaseArgumentCount(any())).thenReturn(Unit)
        whenever(debateTopicService.getTopicDetail(any(), any())).thenReturn(null)
        whenever(fallacyDetectionService.detectFallacyAsync(any(), any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(null))

        val result = debateArgumentService.createArgument(argumentEntity, fileIds)

        assertThat(result).isNotNull
        verify(debateArgumentRepository).save(any())
        verify(debateTopicService).increaseArgumentCount(any())
    }

    @Test
    fun `createArgument - 내용 길이 초과 시 예외 발생`() {
        val longContent = "a".repeat(5001)
        val longContentEntity = argumentEntity.copy(content = longContent)

        assertThatThrownBy {
            debateArgumentService.createArgument(longContentEntity, null)
        }.isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.WRONG_ACCESS
            }
    }

    @Test
    fun `deleteArgument - 성공 케이스`() {
        val topicId = 1L
        val argumentId = 1L
        val userId = 100L

        whenever(debateArgumentRepository.findByIdAndTopicIdAndActiveYnTrue(argumentId, topicId))
            .thenReturn(argumentEntity)
        whenever(debateArgumentRepository.softDelete(argumentId, userId)).thenReturn(1)
        whenever(debateTopicService.decreaseArgumentCount(topicId)).thenReturn(Unit)

        debateArgumentService.deleteArgument(topicId, argumentId, userId)

        verify(debateTopicService).decreaseArgumentCount(topicId)
    }

    @Test
    fun `deleteArgument - 작성자가 아닌 경우 예외 발생`() {
        val topicId = 1L
        val argumentId = 1L
        val otherUserId = 999L

        whenever(debateArgumentRepository.findByIdAndTopicIdAndActiveYnTrue(argumentId, topicId))
            .thenReturn(argumentEntity)

        assertThatThrownBy {
            debateArgumentService.deleteArgument(topicId, argumentId, otherUserId)
        }.isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.DEBATE_ARGUMENT_DELETE_DENIED
            }
    }

    @Test
    fun `getArgument - 성공 케이스`() {
        val topicId = 1L
        val argumentId = 1L

        whenever(debateArgumentRepository.findByIdAndTopicIdAndActiveYnTrue(argumentId, topicId))
            .thenReturn(argumentEntity)

        val result = debateArgumentService.getArgument(topicId, argumentId, false)

        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(argumentId)
    }

    @Test
    fun `getArgument - 논증을 찾을 수 없는 경우`() {
        val topicId = 1L
        val argumentId = 999L

        whenever(debateArgumentRepository.findByIdAndTopicIdAndActiveYnTrue(argumentId, topicId))
            .thenReturn(null)

        val result = debateArgumentService.getArgument(topicId, argumentId, false)

        assertThat(result).isNull()
    }

    @Test
    fun `getPopularList - 성공 케이스`() {
        val topicId = 1L

        whenever(debateArgumentRepository.findPopularByStance(topicId, any())).thenReturn(emptyList())

        val result = debateArgumentService.getPopularList(topicId)

        assertThat(result).isNotNull
        assertThat(result.keys).containsAll(ArgumentStance.values().toList())
    }

    @Test
    fun `getPageableList - 성공 케이스`() {
        val topicId = 1L
        val stance = ArgumentStance.PRO
        val pageNo = 0
        val limit = 20

        whenever(debateArgumentRepository.countByTopicIdAndStanceAndActiveYnTrue(topicId, stance)).thenReturn(10L)
        whenever(debateArgumentRepository.findByTopicIdAndStanceAndActiveYnTrueOrderByCreatedAtDesc(any(), any(), any()))
            .thenReturn(listOf(argumentEntity))

        val result = debateArgumentService.getPageableList(topicId, stance, pageNo, limit)

        assertThat(result).isNotNull
        assertThat(result.totalCount).isEqualTo(10)
    }
}

