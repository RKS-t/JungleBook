package org.example.junglebook.service.post

import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.apache.commons.io.FilenameUtils
import org.example.junglebook.entity.post.BoardEntity
import org.example.junglebook.entity.post.PostCountHistoryEntity
import org.example.junglebook.entity.post.PostEntity
import org.example.junglebook.entity.post.PostFileEntity
import org.example.junglebook.enums.post.CountType
import org.example.junglebook.enums.post.PostReferenceType
import org.example.junglebook.repository.post.PostCountHistoryRepository
import org.example.junglebook.repository.post.PostFileRepository
import org.example.junglebook.repository.post.PostRepository
import org.example.junglebook.service.MemberService
import org.example.junglebook.util.logger
import org.example.junglebook.web.dto.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

@Service
class PostService(
    private val postRepository: PostRepository,
    private val postCountHistoryRepository: PostCountHistoryRepository,
    private val postFileRepository: PostFileRepository,
    private val memberService: MemberService
    // TODO: S3Service 구현 필요
    // private val s3Service: S3Service
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

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun increaseViewCount(id: Long) {
        postRepository.increaseViewCount(id)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun increaseCount(board: BoardEntity, id: Long, userId: Long, countType: CountType): Int {
        val existCount = postCountHistoryRepository.countByRefTypeAndRefIdAndUserId(
            PostReferenceType.POST, id, userId
        )

        if (existCount > 0) {
            logger().warn("Already exists count history for postId: {}, userId: {}", id, userId)
            throw GlobalException(DefaultErrorCode.ALREADY_EXISTS)
        }

        val updateResult = when (countType) {
            CountType.LIKE -> postRepository.increaseLikeCount(id)
            CountType.DISLIKE -> {
                logger().warn("DISLIKE count type is not supported for postId: {}", id)
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "싫어요 기능은 PostLikeEntity로 관리되지 않습니다.")
            }
            else -> {
                logger().error("Invalid count type: {} for postId: {}", countType, id)
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
            }
        }

        if (updateResult <= 0) {
            logger().error("Failed to increase count for postId: {}, countType: {}", id, countType)
            throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
        }

        val history = PostCountHistoryEntity(
            refType = PostReferenceType.POST,
            refId = id,
            userId = userId,
            type = countType
        )
        postCountHistoryRepository.save(history)

        val updatedPost = postRepository.findById(id)
            .orElseGet {
                logger().error("Post not found after update: postId: {}", id)
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
            }

        return when (countType) {
            CountType.LIKE -> updatedPost.likeCnt
            CountType.DISLIKE -> {
                logger().error("DISLIKE count type should not reach here for postId: {}", id)
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
            }
            else -> {
                logger().error("Invalid count type in return: {} for postId: {}", countType, id)
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
            }
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun createPost(boardId: Int, request: PostCreateRequest, userId: Long): PostResponse {
        val entity = request.toEntity(boardId, userId, request.authorNickname ?: "익명")
        val savedEntity = postRepository.save(entity)
        val savedEntityId = requireNotNull(savedEntity.id) { "Saved post ID must not be null" }

        request.fileIds?.forEach { fileId ->
            postFileRepository.updateAttachStatus(
                refType = PostReferenceType.POST.value,
                refId = savedEntityId,
                id = fileId,
                userId = savedEntity.userId
            )
        }
        
        return PostResponse.of(savedEntity)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun createPost(entity: PostEntity, fileIds: List<Long>?) {
        val savedEntity = postRepository.save(entity)

        fileIds?.forEach { fileId ->
            postFileRepository.updateAttachStatus(
                refType = PostReferenceType.POST.value,
                refId = savedEntity.id,
                id = fileId,
                userId = savedEntity.userId
            )
        }
    }

    @Transactional(readOnly = true)
    fun getPostDetail(postId: Long, increaseView: Boolean = true): PostDetailResponse? {
        val post = postRepository.findByIdAndUseYnTrue(postId) ?: return null
        
        if (increaseView) {
            postRepository.increaseViewCount(postId)
        }
        
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

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun updatePost(postId: Long, request: PostUpdateRequest, userId: Long): PostResponse? {
        val post = postRepository.findByIdAndUseYnTrue(postId) ?: return null
        
        if (post.userId != userId) {
            logger().warn("Unauthorized update attempt: postId: {}, userId: {}, postOwnerId: {}", postId, userId, post.userId)
            throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "작성자만 수정할 수 있습니다.")
        }
        
        request.title?.let { post.title = it }
        request.content?.let { post.content = it }
        request.contentHtml?.let { post.contentHtml = it }
        
        val saved = postRepository.save(post)
        return PostResponse.of(saved)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun deletePost(postId: Long, userId: Long) {
        val post = postRepository.findByIdAndUseYnTrue(postId)
            ?: throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "게시글을 찾을 수 없습니다.")
        
        if (post.userId != userId) {
            logger().warn("Unauthorized delete attempt: postId: {}, userId: {}, postOwnerId: {}", postId, userId, post.userId)
            throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "작성자만 삭제할 수 있습니다.")
        }
        
        post.softDelete()
        postRepository.save(post)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun updatePost(entity: PostEntity, newFileIds: List<Long>?, delFileIds: List<Long>?) {
        delFileIds?.forEach { fileId ->
            postFileRepository.deleteByIdAndUserId(fileId, entity.userId)
        }

        newFileIds?.forEach { fileId ->
            postFileRepository.updateAttachStatus(
                refType = PostReferenceType.POST.value,
                refId = entity.id,
                id = fileId,
                userId = entity.userId
            )
        }

        postRepository.save(entity)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun insertPostFile(boardId: Int, attachYn: Boolean, userId: Long, file: MultipartFile): String {
        val newFileName = "${Instant.now().toEpochMilli()}" +
                String.format("%06d", ThreadLocalRandom.current().nextInt(1000000)) +
                ".${FilenameUtils.getExtension(file.originalFilename)}"

        val url = "/temp/post/$boardId/$newFileName"

        val entity = PostFileEntity(
            url = url,
            attachYn = attachYn,
            fileName = file.originalFilename,
            fileSize = file.size.toString(),
            fileType = file.contentType,
            userId = userId
        )

        postFileRepository.saveAll(listOf(entity))

        return url
    }
}