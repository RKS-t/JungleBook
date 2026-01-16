package org.example.junglebook.web.controller.post

import org.assertj.core.api.Assertions.assertThat
import org.example.junglebook.model.Member
import org.example.junglebook.service.MemberService
import org.example.junglebook.service.post.PostCommandService
import org.example.junglebook.service.post.PostQueryService
import org.example.junglebook.web.dto.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@ExtendWith(MockitoExtension::class)
class PostControllerTest {

    @Mock
    private lateinit var postCommandService: PostCommandService

    @Mock
    private lateinit var postQueryService: PostQueryService

    @Mock
    private lateinit var memberService: MemberService

    @InjectMocks
    private lateinit var postController: PostController

    private lateinit var member: Member
    private lateinit var postResponse: PostResponse
    private lateinit var memberEntity: org.example.junglebook.entity.MemberEntity

    @BeforeEach
    fun setUp() {
        memberEntity = org.example.junglebook.entity.MemberEntity(
            id = 100L,
            loginId = "testuser",
            name = "테스트유저",
            birth = "1990-01-01",
            phoneNumber = "01012345678",
            email = "test@example.com",
            sex = org.example.junglebook.enums.Sex.M,
            ideology = org.example.junglebook.enums.Ideology.M,
            nickname = "테스트유저",
            password = "encodedPassword",
            profileImage = "",
            deleteYn = 0,
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = java.time.LocalDateTime.now()
        )
        member = Member.from(memberEntity)

        postResponse = PostResponse(
            id = 1L,
            boardId = 1,
            userId = 100L,
            title = "테스트 제목",
            content = "테스트 내용",
            authorNickname = "테스트유저",
            contentHtml = null,
            viewCnt = 0,
            likeCnt = 0,
            replyCnt = 0,
            noticeYn = false,
            fileYn = false,
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = java.time.LocalDateTime.now()
        )
    }

    @Test
    fun `createPost - 성공 케이스`() {
        val request = PostCreateRequest(
            title = "새 게시글",
            content = "내용"
        )
        val boardId = 1

        whenever(memberService.getMemberId(member)).thenReturn(100L)
        whenever(postCommandService.createPost(boardId, request, 100L)).thenReturn(postResponse.copy(title = request.title))

        val result = postController.createPost(member, boardId, request)

        assertThat(result.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(result.body).isNotNull
        assertThat(result.body?.title).isEqualTo(request.title)
    }

    @Test
    fun `getPostDetail - 성공 케이스`() {
        val postId = 1L
        val postDetail = PostDetailResponse(
            post = postResponse,
            files = emptyList()
        )

        whenever(postQueryService.getPostDetail(postId)).thenReturn(postDetail)

        val result = postController.getPostDetail(postId, true)

        assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(result.body).isNotNull
        assertThat(result.body?.post?.id).isEqualTo(postId)
    }

    @Test
    fun `getPostDetail - 게시글을 찾을 수 없는 경우`() {
        val postId = 999L

        whenever(postQueryService.getPostDetail(postId)).thenReturn(null)

        val result = postController.getPostDetail(postId, true)

        assertThat(result.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `getPostList - 성공 케이스`() {
        val boardId = 1
        val sortType = PostSortType.LATEST
        val pageNo = 0
        val limit = 20
        val postList = PostListResponse(
            totalCount = 10,
            pageNo = 0,
            posts = listOf(
                PostListResponse.Post(
                    id = postResponse.id,
                    noticeYn = postResponse.noticeYn,
                    fileYn = postResponse.fileYn,
                    authorNickname = postResponse.authorNickname,
                    title = postResponse.title,
                    viewCnt = postResponse.viewCnt,
                    likeCnt = postResponse.likeCnt,
                    replyCnt = postResponse.replyCnt,
                    createdAt = postResponse.createdAt
                )
            )
        )

        whenever(postQueryService.getPostList(boardId, sortType, pageNo, limit, null))
            .thenReturn(postList)

        val result = postController.getPostList(boardId, sortType, pageNo, limit, null)

        assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(result.body).isNotNull
        assertThat(result.body?.totalCount).isEqualTo(10)
    }

    @Test
    fun `updatePost - 성공 케이스`() {
        val postId = 1L
        val request = PostUpdateRequest(
            title = "수정된 제목",
            content = "수정된 내용"
        )
        val updatedResponse = postResponse.copy(title = request.title!!)

        whenever(memberService.getMemberId(member)).thenReturn(100L)
        whenever(postCommandService.updatePost(postId, request, 100L)).thenReturn(updatedResponse)

        val result = postController.updatePost(member, postId, request)

        assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(result.body).isNotNull
        assertThat(result.body?.title).isEqualTo(request.title)
    }

    @Test
    fun `deletePost - 성공 케이스`() {
        val postId = 1L

        whenever(memberService.getMemberId(member)).thenReturn(100L)

        val result = postController.deletePost(member, postId)

        assertThat(result.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }
}

