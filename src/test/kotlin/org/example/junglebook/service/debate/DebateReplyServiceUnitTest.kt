package org.example.junglebook.service.debate

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.example.junglebook.entity.debate.DebateReplyEntity
import org.example.junglebook.enums.ArgumentStance
import org.example.junglebook.enums.DebateReferenceType
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateFileRepository
import org.example.junglebook.repository.debate.DebateReplyRepository
import org.example.junglebook.web.dto.DebateReplyResponse
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

@ExtendWith(MockitoExtension::class)
class DebateReplyServiceUnitTest {

    @Mock
    private lateinit var debateReplyRepository: DebateReplyRepository

    @Mock
    private lateinit var debateArgumentRepository: DebateArgumentRepository

    @Mock
    private lateinit var debateFileRepository: DebateFileRepository

    @InjectMocks
    private lateinit var debateReplyService: DebateReplyService

    private lateinit var replyEntity: DebateReplyEntity

    @BeforeEach
    fun setUp() {
        replyEntity = DebateReplyEntity(
            id = 1L,
            argumentId = 1L,
            userId = 100L,
            content = "테스트 댓글",
            authorNickname = "테스트유저",
            parentId = null,
            depth = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `createReply - 성공 케이스`() {
        val fileIds = listOf(1L, 2L)
        val savedEntity = replyEntity.copy(id = 1L)

        whenever(debateReplyRepository.save(any())).thenReturn(savedEntity)
        whenever(debateFileRepository.updateAttachStatus(any(), any(), any(), any())).thenReturn(1)
        whenever(debateArgumentRepository.increaseReplyCount(any())).thenReturn(1)

        val result = debateReplyService.createReply(replyEntity, fileIds)

        assertThat(result).isNotNull
        verify(debateReplyRepository).save(any())
        verify(debateArgumentRepository).increaseReplyCount(any())
    }

    @Test
    fun `createReply - 부모 댓글이 없는 경우 성공`() {
        val savedEntity = replyEntity.copy(id = 1L, parentId = null)

        whenever(debateReplyRepository.save(any())).thenReturn(savedEntity)
        whenever(debateArgumentRepository.increaseReplyCount(any())).thenReturn(1)

        val result = debateReplyService.createReply(replyEntity, null)

        assertThat(result).isNotNull
        verify(debateReplyRepository, never()).findByIdAndActiveYnTrue(any())
    }

    @Test
    fun `createReply - 깊이 제한 초과 시 예외 발생`() {
        val parentReply = replyEntity.copy(id = 2L, depth = 1)
        val childReply = replyEntity.copy(parentId = 2L, depth = 1)

        whenever(debateReplyRepository.findByIdAndActiveYnTrue(2L)).thenReturn(parentReply)

        assertThatThrownBy {
            debateReplyService.createReply(childReply, null)
        }.isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.REPLY_DEPTH_LIMIT_EXCEEDED
            }
    }

    @Test
    fun `deleteReply - 성공 케이스`() {
        val replyId = 1L
        val userId = 100L

        whenever(debateReplyRepository.findByIdAndActiveYnTrue(replyId)).thenReturn(replyEntity)
        whenever(debateReplyRepository.softDelete(replyId, userId)).thenReturn(1)
        whenever(debateArgumentRepository.decreaseReplyCount(any())).thenReturn(1)

        debateReplyService.deleteReply(replyId, userId)

        verify(debateArgumentRepository).decreaseReplyCount(any())
    }

    @Test
    fun `deleteReply - 작성자가 아닌 경우 예외 발생`() {
        val replyId = 1L
        val otherUserId = 999L

        whenever(debateReplyRepository.findByIdAndActiveYnTrue(replyId)).thenReturn(replyEntity)

        assertThatThrownBy {
            debateReplyService.deleteReply(replyId, otherUserId)
        }.isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.FORBIDDEN
            }
    }

    @Test
    fun `getReply - 성공 케이스`() {
        val replyId = 1L

        whenever(debateReplyRepository.findByIdAndActiveYnTrue(replyId)).thenReturn(replyEntity)

        val result = debateReplyService.getReply(replyId)

        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(replyId)
    }

    @Test
    fun `getReply - 댓글을 찾을 수 없는 경우 예외 발생`() {
        val replyId = 999L

        whenever(debateReplyRepository.findByIdAndActiveYnTrue(replyId)).thenReturn(null)

        assertThatThrownBy {
            debateReplyService.getReply(replyId)
        }.isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.REPLY_NOT_FOUND
            }
    }

    @Test
    fun `getRepliesByArgument - 성공 케이스`() {
        val argumentId = 1L
        val pageNo = 0
        val limit = 20

        whenever(debateReplyRepository.countByArgumentIdAndActiveYnTrue(argumentId)).thenReturn(10L)
        whenever(debateReplyRepository.findByArgumentIdAndActiveYnTrueOrderByCreatedAtDesc(any(), any()))
            .thenReturn(listOf(replyEntity))

        val result = debateReplyService.getRepliesByArgument(argumentId, pageNo, limit)

        assertThat(result).isNotNull
        assertThat(result.totalCount).isEqualTo(10)
    }
}

