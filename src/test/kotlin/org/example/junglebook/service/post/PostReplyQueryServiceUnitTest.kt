package org.example.junglebook.service.post

import org.assertj.core.api.Assertions.assertThat
import org.example.junglebook.entity.post.PostReplyEntity
import org.example.junglebook.repository.post.PostReplyRepository
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
class PostReplyQueryServiceUnitTest {

    @Mock
    private lateinit var postReplyRepository: PostReplyRepository

    @InjectMocks
    private lateinit var postReplyQueryService: PostReplyQueryService

    private lateinit var replyEntity: PostReplyEntity

    @BeforeEach
    fun setUp() {
        replyEntity = PostReplyEntity(
            id = 1L,
            boardId = 1,
            postId = 1L,
            userId = 100L,
            content = "댓글 내용",
            contentHtml = "<p>댓글 내용</p>",
            authorNickname = "테스트유저",
            createdDt = LocalDateTime.now(),
            updatedDt = LocalDateTime.now()
        )
    }

    @Test
    fun `postReplyList - 성공 케이스`() {
        whenever(postReplyRepository.findByPostIdAndUseYnTrueOrderByCreatedDtAsc(any(), any()))
            .thenReturn(listOf(replyEntity))

        val result = postReplyQueryService.postReplyList(1L)

        assertThat(result).isNotNull
        assertThat(result.size).isEqualTo(1)
    }
}
