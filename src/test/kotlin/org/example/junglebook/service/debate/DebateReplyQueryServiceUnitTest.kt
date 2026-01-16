package org.example.junglebook.service.debate

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.example.junglebook.entity.debate.DebateReplyEntity
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.repository.debate.DebateReplyRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class DebateReplyQueryServiceUnitTest {

    @Mock
    private lateinit var debateReplyRepository: DebateReplyRepository

    @InjectMocks
    private lateinit var debateReplyQueryService: DebateReplyQueryService

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
    fun `getReply - 성공 케이스`() {
        val replyId = 1L

        whenever(debateReplyRepository.findByIdAndActiveYnTrue(replyId)).thenReturn(replyEntity)

        val result = debateReplyQueryService.getReply(replyId)

        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(replyId)
    }

    @Test
    fun `getReply - 댓글을 찾을 수 없는 경우 예외 발생`() {
        val replyId = 999L

        whenever(debateReplyRepository.findByIdAndActiveYnTrue(replyId)).thenReturn(null)

        assertThatThrownBy {
            debateReplyQueryService.getReply(replyId)
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

        val result = debateReplyQueryService.getRepliesByArgument(argumentId, pageNo, limit)

        assertThat(result).isNotNull
        assertThat(result.totalCount).isEqualTo(10)
    }
}
