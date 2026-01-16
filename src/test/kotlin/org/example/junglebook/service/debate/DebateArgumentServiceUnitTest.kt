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
import org.example.junglebook.repository.debate.DebateTopicRepository
import org.example.junglebook.service.fallacy.FallacyDetectionService
import org.example.junglebook.web.dto.DebateArgumentCreateRequest
import org.springframework.transaction.support.TransactionTemplate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@ExtendWith(MockitoExtension::class)
class DebateArgumentCommandServiceUnitTest {

    @Mock
    private lateinit var debateArgumentRepository: DebateArgumentRepository

    @Mock
    private lateinit var debateFileRepository: DebateFileRepository

    @Mock
    private lateinit var debateTopicCommandService: DebateTopicCommandService

    @Mock
    private lateinit var debateTopicRepository: DebateTopicRepository

    @Mock
    private lateinit var fallacyDetectionService: FallacyDetectionService

    @Mock
    private lateinit var transactionTemplate: TransactionTemplate

    @InjectMocks
    private lateinit var debateArgumentCommandService: DebateArgumentCommandService

    private val argumentEntity = DebateArgumentEntity(
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

    @Test
    fun `createArgument - 성공 케이스`() {
        val fileIds = listOf(1L, 2L)
        val savedEntity = argumentEntity.copy(id = 1L)
        val request = DebateArgumentCreateRequest(
            stance = ArgumentStance.PRO,
            title = "테스트 논증",
            content = "테스트 내용",
            contentHtml = null,
            authorNickname = "테스트유저",
            fileIds = fileIds
        )

        doReturn(savedEntity).whenever(debateArgumentRepository).save(any(DebateArgumentEntity::class.java))
        whenever(debateFileRepository.updateAttachStatus(anyInt(), anyLong(), anyLong(), anyLong())).thenReturn(1)
        doNothing().whenever(debateTopicCommandService).increaseArgumentCount(anyLong())
        whenever(debateTopicRepository.findByIdAndActiveYnTrue(anyLong())).thenReturn(null)
        whenever(fallacyDetectionService.detectFallacyAsync(
            anyString(),
            anyString(),
            anyOrNull(),
            anyOrNull()
        ))
            .thenReturn(CompletableFuture.completedFuture(null))

        val result = debateArgumentCommandService.createArgument(1L, 100L, request)

        assertThat(result).isNotNull
        verify(debateArgumentRepository).save(any(DebateArgumentEntity::class.java))
        verify(debateTopicCommandService).increaseArgumentCount(anyLong())
    }

    @Test
    fun `deleteArgument - 성공 케이스`() {
        val topicId = 1L
        val argumentId = 1L
        val userId = 100L

        whenever(debateArgumentRepository.findByIdAndTopicIdAndActiveYnTrue(argumentId, topicId))
            .thenReturn(argumentEntity)
        whenever(debateArgumentRepository.softDelete(argumentId, userId)).thenReturn(1)
        doNothing().whenever(debateTopicCommandService).decreaseArgumentCount(eq(topicId))

        debateArgumentCommandService.deleteArgument(topicId, argumentId, userId)

        verify(debateTopicCommandService).decreaseArgumentCount(topicId)
    }

    @Test
    fun `deleteArgument - 작성자가 아닌 경우 예외 발생`() {
        val topicId = 1L
        val argumentId = 1L
        val otherUserId = 999L

        whenever(debateArgumentRepository.findByIdAndTopicIdAndActiveYnTrue(argumentId, topicId))
            .thenReturn(argumentEntity)

        assertThatThrownBy {
            debateArgumentCommandService.deleteArgument(topicId, argumentId, otherUserId)
        }.isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.DEBATE_ARGUMENT_DELETE_DENIED
            }
    }
}

