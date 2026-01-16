package org.example.junglebook.service.post

import org.assertj.core.api.Assertions.assertThat
import org.example.junglebook.entity.post.PostEntity
import org.example.junglebook.entity.post.PostFileEntity
import org.example.junglebook.enums.post.PostReferenceType
import org.example.junglebook.repository.post.PostFileRepository
import org.example.junglebook.repository.post.PostRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class PostQueryServiceUnitTest {

    @Mock
    private lateinit var postRepository: PostRepository

    @Mock
    private lateinit var postFileRepository: PostFileRepository

    @InjectMocks
    private lateinit var postQueryService: PostQueryService

    private lateinit var postEntity: PostEntity

    @BeforeEach
    fun setUp() {
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
    fun `getPostDetail - 성공 케이스`() {
        val postId = 1L
        val fileEntity = PostFileEntity(
            id = 1L,
            refType = PostReferenceType.POST.value,
            refId = postId,
            url = "/temp/post/1/test.png",
            attachYn = true,
            fileName = "test.png",
            fileSize = "123",
            fileType = "image/png",
            userId = 100L
        )

        whenever(postRepository.findByIdAndUseYnTrue(postId)).thenReturn(postEntity)
        whenever(postFileRepository.findByRefTypeAndRefId(PostReferenceType.POST.value, postId)).thenReturn(listOf(fileEntity))

        val result = postQueryService.getPostDetail(postId)

        assertThat(result).isNotNull
        assertThat(result?.post?.id).isEqualTo(postId)
        assertThat(result?.files?.size).isEqualTo(1)
    }

    @Test
    fun `getPostDetail - 게시글을 찾을 수 없는 경우`() {
        val postId = 999L

        whenever(postRepository.findByIdAndUseYnTrue(postId)).thenReturn(null)

        val result = postQueryService.getPostDetail(postId)

        assertThat(result).isNull()
    }
}
