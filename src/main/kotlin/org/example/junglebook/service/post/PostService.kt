package org.example.junglebook.service.post

import kr.co.minust.api.exception.DefaultErrorCode
import kr.co.minust.api.exception.GlobalException
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
import org.example.junglebook.web.dto.PostListResponse
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
    private val s3Service: S3Service
) {

    @Transactional(readOnly = true)
    fun pageablePostList(board: BoardEntity, pageNo: Int, searchType: Int, searchValue: String?, limit: Int): PostListResponse {
        val pageable = PageRequest.of(pageNo, limit)
        val list = postRepository.findPageableList(board.id!!, searchType, searchValue, pageable)
        val totalCount = postRepository.countByBoardIdWithSearch(board.id!!, searchType, searchValue)

        return PostListResponse.of(totalCount, pageNo, list)
    }

    @Transactional(readOnly = true)
    fun post(id: Long): PostEntity? {
        return postRepository.findById(id).orElse(null)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun increaseViewCount(boardId: Int, id: Long) {
        postRepository.increaseViewCount(boardId, id)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun increaseCount(board: BoardEntity, id: Long, userId: Long, countType: CountType): Int {
        val existCount = postCountHistoryRepository.countByRefTypeAndRefIdAndUserId(
            PostReferenceType.POST, id, userId
        )

        if (existCount > 0) {
            throw GlobalException(DefaultErrorCode.ALREADY_EXISTS)
        }

        val updateResult = when (countType) {
            CountType.LIKE -> postRepository.increaseLikeCount(board.id!!, id)
            CountType.DISLIKE -> postRepository.increaseDislikeCount(board.id!!, id)
            else -> throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
        }

        if (updateResult > 0) {
            val history = PostCountHistoryEntity(
                refType = PostReferenceType.POST,
                refId = id,
                userId = userId,
                type = countType
            )
            postCountHistoryRepository.save(history)

            val updatedPost = postRepository.findById(id)
                .orElseThrow { GlobalException(DefaultErrorCode.WRONG_ACCESS) }

            return when (countType) {
                CountType.LIKE -> updatedPost.likeCnt
                CountType.DISLIKE -> updatedPost.dislikeCnt
                else -> throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
            }
        } else {
            throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
        }
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

        val url = s3Service.saveFile(file, "/post/$boardId/$newFileName")

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