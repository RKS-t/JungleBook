package org.example.junglebook.service.post

import org.example.junglebook.entity.post.BoardEntity
import org.example.junglebook.entity.post.PostEntity
import org.example.junglebook.enums.post.PostReferenceType
import org.example.junglebook.repository.post.PostFileRepository
import org.example.junglebook.repository.post.PostRepository
import org.example.junglebook.web.dto.PostDetailResponse
import org.example.junglebook.web.dto.PostFileResponse
import org.example.junglebook.web.dto.PostListResponse
import org.example.junglebook.web.dto.PostResponse
import org.example.junglebook.web.dto.PostSimpleResponse
import org.example.junglebook.web.dto.PostSortType
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostQueryService(
    private val postRepository: PostRepository,
    private val postFileRepository: PostFileRepository
) {

    @Transactional(readOnly = true)
    fun pageablePostList(board: BoardEntity, pageNo: Int, searchType: Int, searchValue: String?, limit: Int): PostListResponse {
        val boardId = requireNotNull(board.id) { "Board ID must not be null" }
        val pageable = PageRequest.of(pageNo, limit)
        val list = postRepository.findPageableList(boardId, searchType, searchValue, pageable)
        val totalCount = postRepository.countByBoardIdWithSearch(boardId, searchType, searchValue)

        return PostListResponse.of(totalCount, pageNo, list)
    }

    @Transactional(readOnly = true)
    fun post(id: Long): PostEntity? {
        return postRepository.findById(id).orElse(null)
    }

    @Transactional(readOnly = true)
    fun getPostDetail(postId: Long): PostDetailResponse? {
        val post = postRepository.findByIdAndUseYnTrue(postId) ?: return null
        val fileEntities = postFileRepository.findByRefTypeAndRefId(PostReferenceType.POST.value, postId)
        val files = fileEntities.map { PostFileResponse.of(it) }

        return PostDetailResponse(
            post = PostResponse.of(post),
            files = files
        )
    }

    @Transactional(readOnly = true)
    fun getPostList(
        boardId: Int,
        sortType: PostSortType,
        pageNo: Int,
        limit: Int,
        keyword: String?
    ): PostListResponse {
        val pageable = PageRequest.of(pageNo, limit)

        val posts = when (sortType) {
            PostSortType.LATEST -> {
                if (keyword != null) {
                    postRepository.searchByKeyword(boardId, keyword, pageable)
                } else {
                    postRepository.findByBoardIdAndUseYnTrueOrderByNoticeYnDescCreatedDtDesc(boardId, pageable)
                }
            }
            PostSortType.POPULAR -> {
                postRepository.findPopularByBoardId(boardId, pageable)
            }
            PostSortType.MOST_VIEWED -> {
                postRepository.findByBoardIdAndUseYnTrueOrderByViewCntDesc(boardId, pageable)
            }
            PostSortType.MOST_LIKED -> {
                postRepository.findByBoardIdAndUseYnTrueOrderByLikeCntDesc(boardId, pageable)
            }
        }

        val totalCount = postRepository.countByBoardIdAndUseYnTrue(boardId).toInt()

        return PostListResponse.of(totalCount, pageNo, posts)
    }

    @Transactional(readOnly = true)
    fun getPopularPosts(boardId: Int, limit: Int): List<PostSimpleResponse> {
        val pageable = PageRequest.of(0, limit)
        val posts = postRepository.findPopularByBoardId(boardId, pageable)
        return PostSimpleResponse.of(posts)
    }

    @Transactional(readOnly = true)
    fun getPostsByAuthor(userId: Long, pageNo: Int, limit: Int): PostListResponse {
        val pageable = PageRequest.of(pageNo, limit)
        val posts = postRepository.findByUserIdAndUseYnTrueOrderByCreatedDtDesc(userId, pageable)
        val totalCount = postRepository.countByUserIdAndUseYnTrue(userId).toInt()

        return PostListResponse.of(totalCount, pageNo, posts)
    }
}
