package org.example.junglebook.service.debate

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.example.junglebook.entity.debate.DebateReplyEntity
import org.example.junglebook.enums.DebateReferenceType
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateFileRepository
import org.example.junglebook.repository.debate.DebateReplyRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class DebateReplyCommandServiceUnitTest {

    @Mock
    private lateinit var debateReplyRepository: DebateReplyRepository

    @Mock
    private lateinit var debateArgumentRepository: DebateArgumentRepository

    @Mock
    private lateinit var debateFileRepository: DebateFileRepository

    @InjectMocks
    private lateinit var debateReplyCommandService: DebateReplyCommandService

    private val replyEntity = DebateReplyEntity(
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

    @Test
    fun `createReply - 성공 케이스`() {
        val fileIds = listOf(1L, 2L)
        val savedEntity = replyEntity.copy(id = 1L)

        whenever(debateReplyRepository.save(replyEntity)).thenReturn(savedEntity)
        whenever(debateFileRepository.updateAttachStatus(anyInt(), anyLong(), anyLong(), anyLong())).thenReturn(1)
        whenever(debateArgumentRepository.increaseReplyCount(anyLong())).thenReturn(1)

        val result = debateReplyCommandService.createReply(replyEntity, fileIds)

        assertThat(result).isNotNull
        verify(debateReplyRepository).save(replyEntity)
        verify(debateArgumentRepository).increaseReplyCount(anyLong())
    }

    @Test
    fun `createReply - 부모 댓글이 없는 경우 성공`() {
        val savedEntity = replyEntity.copy(id = 1L, parentId = null)

        whenever(debateReplyRepository.save(replyEntity)).thenReturn(savedEntity)
        whenever(debateArgumentRepository.increaseReplyCount(anyLong())).thenReturn(1)

        val result = debateReplyCommandService.createReply(replyEntity, null)

        assertThat(result).isNotNull
        verify(debateReplyRepository, never()).findByIdAndActiveYnTrue(anyLong())
    }

    @Test
    fun `createReply - 깊이 제한 초과 시 예외 발생`() {
        val parentReply = replyEntity.copy(id = 2L, depth = 1)
        val childReply = replyEntity.copy(parentId = 2L, depth = 1)

        whenever(debateReplyRepository.findByIdAndActiveYnTrue(2L)).thenReturn(parentReply)

        assertThatThrownBy {
            debateReplyCommandService.createReply(childReply, null)
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
        whenever(debateArgumentRepository.decreaseReplyCount(anyLong())).thenReturn(1)

        debateReplyCommandService.deleteReply(replyId, userId)

        verify(debateArgumentRepository).decreaseReplyCount(anyLong())
    }

    @Test
    fun `deleteReply - 작성자가 아닌 경우 예외 발생`() {
        val replyId = 1L
        val otherUserId = 999L

        whenever(debateReplyRepository.findByIdAndActiveYnTrue(replyId)).thenReturn(replyEntity)

        assertThatThrownBy {
            debateReplyCommandService.deleteReply(replyId, otherUserId)
        }.isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.FORBIDDEN
            }
    }
}

