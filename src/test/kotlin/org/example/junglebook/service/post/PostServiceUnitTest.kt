package org.example.junglebook.service.post

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.example.junglebook.entity.post.BoardEntity
import org.example.junglebook.entity.post.PostEntity
import org.example.junglebook.entity.post.PostFileEntity
import org.example.junglebook.enums.post.CountType
import org.example.junglebook.enums.post.PostReferenceType
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.repository.post.PostCountHistoryRepository
import org.example.junglebook.repository.post.PostFileRepository
import org.example.junglebook.repository.post.PostRepository
import org.example.junglebook.service.MemberService
import org.example.junglebook.web.dto.PostCreateRequest
import org.example.junglebook.web.dto.PostResponse
import org.example.junglebook.web.dto.PostSortType
import org.example.junglebook.web.dto.PostUpdateRequest
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
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PostServiceUnitTest {

    @Mock
    private lateinit var postRepository: PostRepository

    @Mock
    private lateinit var postCountHistoryRepository: PostCountHistoryRepository

    @Mock
    private lateinit var postFileRepository: PostFileRepository

    @Mock
    private lateinit var memberService: MemberService

    @InjectMocks
    private lateinit var postService: PostService

    private lateinit var boardEntity: BoardEntity
    private lateinit var postEntity: PostEntity

    @BeforeEach
    fun setUp() {
        boardEntity = BoardEntity(
            id = 1,
            name = "테스트 게시판"
        )

        postEntity = PostEntity(
            id = 1L,
            boardId = 1,
            userId = 100L,
            title = "테스트 제목",
            content = "테스트 내용",
            authorNickname = "테스트유저"
        )
    }

    @Test
    fun `createPost - 성공 케이스`() {
        val request = PostCreateRequest(
            title = "새 게시글",
            content = "내용",
            fileIds = null
        )
        val userId = 100L
        val savedEntity = postEntity.copy(
            id = 1L,
            title = request.title,
            content = request.content
        )

        whenever(postRepository.save(any())).thenReturn(savedEntity)

        val result = postService.createPost(1, request, userId)

        assertThat(result).isNotNull
        assertThat(result.title).isEqualTo(request.title)
        verify(postRepository).save(any())
    }

    @Test
    fun `createPost - 파일 ID가 있는 경우`() {
        val request = PostCreateRequest(
            title = "새 게시글",
            content = "내용",
            fileIds = listOf(1L, 2L)
        )
        val userId = 100L
        val savedEntity = postEntity.copy(
            id = 1L,
            title = request.title,
            content = request.content
        )

        whenever(postRepository.save(any())).thenReturn(savedEntity)
        whenever(postFileRepository.updateAttachStatus(any(), any(), any(), any())).thenReturn(1)

        val result = postService.createPost(1, request, userId)

        assertThat(result).isNotNull
        verify(postFileRepository).updateAttachStatus(
            PostReferenceType.POST.value,
            1L,
            1L,
            userId
        )
        verify(postFileRepository).updateAttachStatus(
            PostReferenceType.POST.value,
            1L,
            2L,
            userId
        )
    }

    @Test
    fun `getPostDetail - 성공 케이스`() {
        val postId = 1L

        whenever(postRepository.findByIdAndUseYnTrue(postId)).thenReturn(postEntity)
        whenever(postRepository.increaseViewCount(postId)).thenReturn(1)

        val result = postService.getPostDetail(postId, increaseView = true)

        assertThat(result).isNotNull
        assertThat(result?.post?.id).isEqualTo(postId)
        verify(postRepository).increaseViewCount(postId)
    }

    @Test
    fun `getPostDetail - 게시글을 찾을 수 없는 경우`() {
        val postId = 999L

        whenever(postRepository.findByIdAndUseYnTrue(postId)).thenReturn(null)

        val result = postService.getPostDetail(postId)

        assertThat(result).isNull()
        verify(postRepository, never()).increaseViewCount(any())
    }

    @Test
    fun `updatePost - 성공 케이스`() {
        val postId = 1L
        val userId = 100L
        val request = PostUpdateRequest(
            title = "수정된 제목",
            content = "수정된 내용"
        )

        val updatedEntity = postEntity.copy(
            title = request.title!!,
            content = request.content!!
        )
        whenever(postRepository.findByIdAndUseYnTrue(postId)).thenReturn(postEntity)
        whenever(postRepository.save(any())).thenReturn(updatedEntity)

        val result = postService.updatePost(postId, request, userId)

        assertThat(result).isNotNull
        assertThat(result?.title).isEqualTo(request.title)
        verify(postRepository).save(any())
    }

    @Test
    fun `updatePost - 작성자가 아닌 경우 예외 발생`() {
        val postId = 1L
        val otherUserId = 999L
        val request = PostUpdateRequest(title = "수정된 제목")

        whenever(postRepository.findByIdAndUseYnTrue(postId)).thenReturn(postEntity)

        assertThatThrownBy {
            postService.updatePost(postId, request, otherUserId)
        }.isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.errorMessage?.contains("작성자만 수정할 수 있습니다") == true
            }
    }

    @Test
    fun `deletePost - 성공 케이스`() {
        val postId = 1L
        val userId = 100L
        val deletedEntity = postEntity.copy(useYn = false)

        whenever(postRepository.findByIdAndUseYnTrue(postId)).thenReturn(postEntity)
        whenever(postRepository.save(any())).thenReturn(deletedEntity)

        postService.deletePost(postId, userId)

        verify(postRepository).save(any())
    }

    @Test
    fun `deletePost - 작성자가 아닌 경우 예외 발생`() {
        val postId = 1L
        val otherUserId = 999L

        whenever(postRepository.findByIdAndUseYnTrue(postId)).thenReturn(postEntity)

        assertThatThrownBy {
            postService.deletePost(postId, otherUserId)
        }.isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.errorMessage?.contains("작성자만 삭제할 수 있습니다") == true
            }
    }

    @Test
    fun `increaseCount - 좋아요 성공 케이스`() {
        val boardId = 1
        val postId = 1L
        val userId = 100L

        whenever(postCountHistoryRepository.countByRefTypeAndRefIdAndUserId(
            PostReferenceType.POST, postId, userId
        )).thenReturn(0)
        whenever(postRepository.increaseLikeCount(postId)).thenReturn(1)
        whenever(postRepository.findById(postId)).thenReturn(
            java.util.Optional.of(postEntity.copy(likeCnt = 1))
        )

        val result = postService.increaseCount(boardEntity, postId, userId, CountType.LIKE)

        assertThat(result).isEqualTo(1)
        verify(postCountHistoryRepository).save(any())
    }

    @Test
    fun `increaseCount - 이미 좋아요한 경우 예외 발생`() {
        val boardId = 1
        val postId = 1L
        val userId = 100L

        whenever(postCountHistoryRepository.countByRefTypeAndRefIdAndUserId(
            PostReferenceType.POST, postId, userId
        )).thenReturn(1)

        assertThatThrownBy {
            postService.increaseCount(boardEntity, postId, userId, CountType.LIKE)
        }.isInstanceOf(GlobalException::class.java)
            .extracting { (it as GlobalException).code }
            .isEqualTo(DefaultErrorCode.ALREADY_EXISTS)
    }

    @Test
    fun `increaseCount - 싫어요는 예외 발생`() {
        val boardId = 1
        val postId = 1L
        val userId = 100L

        whenever(postCountHistoryRepository.countByRefTypeAndRefIdAndUserId(
            PostReferenceType.POST, postId, userId
        )).thenReturn(0)

        assertThatThrownBy {
            postService.increaseCount(boardEntity, postId, userId, CountType.DISLIKE)
        }.isInstanceOf(GlobalException::class.java)
            .extracting { (it as GlobalException).errorMessage }
            .asString()
            .contains("싫어요 기능은 PostLikeEntity로 관리되지 않습니다")
    }
}

