package org.example.junglebook.service.post

import org.assertj.core.api.Assertions.assertThat
import org.example.junglebook.entity.post.PostEntity
import org.example.junglebook.entity.post.PostReplyEntity
import org.example.junglebook.repository.post.PostCountHistoryRepository
import org.example.junglebook.repository.post.PostFileRepository
import org.example.junglebook.repository.post.PostReplyRepository
import org.example.junglebook.repository.post.PostRepository
import org.example.junglebook.service.MemberService
import org.example.junglebook.web.dto.PostReplyCreateRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Answers
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class PostReplyCommandServiceUnitTest {

    @Mock
    private lateinit var postRepository: PostRepository

    private lateinit var postReplyRepository: PostReplyRepository

    @Mock
    private lateinit var postCountHistoryRepository: PostCountHistoryRepository

    @Mock
    private lateinit var postFileRepository: PostFileRepository

    @Mock
    private lateinit var memberService: MemberService

    private lateinit var postReplyCommandService: PostReplyCommandService

    private val postEntity = PostEntity(
        id = 1L,
        boardId = 1,
        userId = 100L,
        title = "테스트 제목",
        content = "테스트 내용",
        authorNickname = "테스트유저"
    )

    private val replyEntity = PostReplyEntity(
        id = 1L,
        boardId = 1,
        postId = 1L,
        userId = 100L,
        content = "댓글 내용",
        contentHtml = "<p>댓글 내용</p>",
        authorNickname = "테스트유저"
    )

    @BeforeEach
    fun setUp() {
        postReplyRepository = Mockito.mock(PostReplyRepository::class.java) { invocation ->
            if (invocation.method.name == "save") {
                replyEntity
            } else {
                Answers.RETURNS_DEFAULTS.answer(invocation)
            }
        }
        postReplyCommandService = PostReplyCommandService(
            postRepository = postRepository,
            postReplyRepository = postReplyRepository,
            postCountHistoryRepository = postCountHistoryRepository,
            postFileRepository = postFileRepository,
            memberService = memberService
        )
    }

    @Test
    fun `create - 성공 케이스`() {
        val request = PostReplyCreateRequest(
            pid = 0,
            contentHtml = "<p>댓글 내용</p>",
            fileIds = null
        )

        whenever(postRepository.findByIdAndUseYnTrue(1L)).thenReturn(postEntity)
        whenever(memberService.getMemberNickname("loginId")).thenReturn("테스트유저")
        val result = postReplyCommandService.create(1L, request, 100L, "loginId")

        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(1L)
    }
}
