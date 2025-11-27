package org.example.junglebook.service.post

import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.entity.post.PostCountHistoryEntity
import org.example.junglebook.entity.post.PostReplyEntity
import org.example.junglebook.enums.post.CountType
import org.example.junglebook.enums.post.PostReferenceType
import org.example.junglebook.repository.post.PostCountHistoryRepository
import org.example.junglebook.repository.post.PostFileRepository
import org.example.junglebook.repository.post.PostReplyRepository
import org.example.junglebook.repository.post.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional


@Service
class PostReplyService(
    private val postRepository: PostRepository,
    private val postReplyRepository: PostReplyRepository,
    private val postCountHistoryRepository: PostCountHistoryRepository,
    private val postFileRepository: PostFileRepository
) {

    @Transactional(readOnly = true)
    fun postReplyList(postId: Long): List<PostReplyEntity> {
        val pageable = org.springframework.data.domain.PageRequest.of(0, 1000) // 최대 1000개
        return postReplyRepository.findByPostIdAndUseYnTrueOrderByCreatedDtAsc(postId, pageable)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun create(entity: PostReplyEntity, fileIds: List<Long>?) {
        val savedEntity = postReplyRepository.save(entity)

        fileIds?.forEach { fileId ->
            postFileRepository.updateAttachStatus(
                refType = PostReferenceType.REPLY.ordinal,
                refId = savedEntity.id,
                id = fileId,
                userId = savedEntity.userId
            )
        }

        postRepository.increaseReplyCount(entity.postId)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun modify(entity: PostReplyEntity, fileIds: List<Long>?): Boolean {
        val replyCount = postReplyRepository.countByParentIdAndUseYnTrue(entity.id!!)

        if (replyCount > 0) {
            throw GlobalException(DefaultErrorCode.REPLY_EXISTS)
        }

        fileIds?.forEach { fileId ->
            postFileRepository.updateAttachStatus(
                refType = PostReferenceType.REPLY.ordinal,
                refId = entity.id,
                id = fileId,
                userId = entity.userId
            )
        }

        return try {
            postReplyRepository.save(entity)
            true
        } catch (e: Exception) {
            false
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun remove(userId: Long, boardId: Int, postId: Long, id: Long): Boolean {
        val replyCount = postReplyRepository.countByParentIdAndUseYnTrue(id)

        if (replyCount > 0) {
            throw GlobalException(DefaultErrorCode.REPLY_EXISTS)
        }

        val reply = postReplyRepository.findByIdAndUseYnTrue(id) ?: return false
        if (reply.userId != userId) {
            throw GlobalException(DefaultErrorCode.FORBIDDEN)
        }
        
        reply.softDelete()
        postReplyRepository.save(reply)
        postRepository.increaseReplyCount(postId)
        
        return true
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun increaseCount(boardId: Int, postId: Long, id: Long, userId: Long, countType: CountType): Int {
        val existCount = postCountHistoryRepository.countByRefTypeAndRefIdAndUserId(
            PostReferenceType.REPLY, id, userId
        )

        if (existCount > 0) {
            throw GlobalException(DefaultErrorCode.ALREADY_EXISTS)
        }

        val updateResult = when (countType) {
            CountType.LIKE -> postReplyRepository.increaseLikeCount(id)
            CountType.DISLIKE -> {
                // 싫어요는 PostLikeEntity로 관리하지 않으므로 별도 처리 필요
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "싫어요 기능은 PostLikeEntity로 관리되지 않습니다.")
            }
            else -> throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
        }

        if (updateResult == 0) {
            // 히스토리 저장
            val history = PostCountHistoryEntity(
                refType = PostReferenceType.REPLY,
                refId = id,
                userId = userId,
                type = countType
            )
            postCountHistoryRepository.save(history)

            // 업데이트된 카운트 반환
            val updatedReply = postReplyRepository.findById(id)
                .orElseThrow { GlobalException(DefaultErrorCode.REPLY_NOT_FOUND) }

            return when (countType) {
                CountType.LIKE -> updatedReply.likeCnt
                CountType.DISLIKE -> updatedReply.dislikeCnt
                else -> throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
            }
        } else {
            throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
        }
    }
}