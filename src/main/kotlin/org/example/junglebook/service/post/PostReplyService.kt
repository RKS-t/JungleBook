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
    fun create(postId: Long, request: org.example.junglebook.web.dto.PostReplyCreateRequest, userId: Long, authorNickname: String): PostReplyEntity {
        val post = postRepository.findByIdAndUseYnTrue(postId)
            ?: throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "게시글을 찾을 수 없습니다.")
        
        val entity = request.toEntity(post.boardId, postId, userId, authorNickname)
        val savedEntity = postReplyRepository.save(entity)

        request.fileIds?.forEach { fileId ->
            postFileRepository.updateAttachStatus(
                refType = PostReferenceType.REPLY.ordinal,
                refId = savedEntity.id,
                id = fileId,
                userId = savedEntity.userId
            )
        }

        postRepository.increaseReplyCount(postId)
        return savedEntity
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun modify(postId: Long, replyId: Long, request: org.example.junglebook.web.dto.PostReplyUpdateRequest, userId: Long, authorNickname: String): PostReplyEntity {
        val reply = postReplyRepository.findByIdAndUseYnTrue(replyId)
            ?: throw GlobalException(DefaultErrorCode.REPLY_NOT_FOUND)
        
        if (reply.userId != userId) {
            throw GlobalException(DefaultErrorCode.FORBIDDEN)
        }
        
        val replyCount = postReplyRepository.countByParentIdAndUseYnTrue(replyId)
        if (replyCount > 0) {
            throw GlobalException(DefaultErrorCode.REPLY_EXISTS)
        }

        val post = postRepository.findByIdAndUseYnTrue(postId)
            ?: throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "게시글을 찾을 수 없습니다.")
        
        val entity = request.toEntity(post.boardId, postId, replyId, userId, authorNickname)
        
        request.fileIds?.forEach { fileId ->
            postFileRepository.updateAttachStatus(
                refType = PostReferenceType.REPLY.ordinal,
                refId = entity.id,
                id = fileId,
                userId = entity.userId
            )
        }

        return postReplyRepository.save(entity)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun remove(postId: Long, replyId: Long, userId: Long) {
        val replyCount = postReplyRepository.countByParentIdAndUseYnTrue(replyId)

        if (replyCount > 0) {
            throw GlobalException(DefaultErrorCode.REPLY_EXISTS)
        }

        val reply = postReplyRepository.findByIdAndUseYnTrue(replyId)
            ?: throw GlobalException(DefaultErrorCode.REPLY_NOT_FOUND)
        
        if (reply.userId != userId) {
            throw GlobalException(DefaultErrorCode.FORBIDDEN)
        }
        
        reply.softDelete()
        postReplyRepository.save(reply)
        postRepository.increaseReplyCount(postId)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun increaseCount(postId: Long, replyId: Long, userId: Long, countType: CountType): Int {
        val post = postRepository.findByIdAndUseYnTrue(postId)
            ?: throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "게시글을 찾을 수 없습니다.")
        val existCount = postCountHistoryRepository.countByRefTypeAndRefIdAndUserId(
            PostReferenceType.REPLY, replyId, userId
        )

        if (existCount > 0) {
            throw GlobalException(DefaultErrorCode.ALREADY_EXISTS)
        }

        val updateResult = when (countType) {
            CountType.LIKE -> postReplyRepository.increaseLikeCount(replyId)
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
                refId = replyId,
                userId = userId,
                type = countType
            )
            postCountHistoryRepository.save(history)

            val updatedReply = postReplyRepository.findById(replyId)
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