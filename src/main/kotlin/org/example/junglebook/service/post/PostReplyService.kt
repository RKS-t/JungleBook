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
import org.example.junglebook.service.MemberService
import org.example.junglebook.util.logger
import org.example.junglebook.web.dto.PostReplyResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional


@Service
class PostReplyService(
    private val postRepository: PostRepository,
    private val postReplyRepository: PostReplyRepository,
    private val postCountHistoryRepository: PostCountHistoryRepository,
    private val postFileRepository: PostFileRepository,
    private val memberService: MemberService
) {

    @Transactional(readOnly = true)
    fun postReplyList(postId: Long): List<PostReplyResponse> {
        val pageable = org.springframework.data.domain.PageRequest.of(0, 1000)
        val replies = postReplyRepository.findByPostIdAndUseYnTrueOrderByCreatedDtAsc(postId, pageable)
        return replies.map { PostReplyResponse.of(it) }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun create(postId: Long, request: org.example.junglebook.web.dto.PostReplyCreateRequest, userId: Long, loginId: String): PostReplyResponse {
        val post = postRepository.findByIdAndUseYnTrue(postId)
            ?: run {
                logger().warn("Post not found for reply creation: postId={}", postId)
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "게시글을 찾을 수 없습니다.")
            }
        
        val authorNickname = memberService.getMemberNickname(loginId)
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
        return PostReplyResponse.of(savedEntity)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun modify(postId: Long, replyId: Long, request: org.example.junglebook.web.dto.PostReplyUpdateRequest, userId: Long, loginId: String): PostReplyResponse {
        val reply = postReplyRepository.findByIdAndUseYnTrue(replyId)
            ?: run {
                logger().warn("Reply not found for modify: replyId={}", replyId)
                throw GlobalException(DefaultErrorCode.REPLY_NOT_FOUND)
            }
        
        if (reply.userId != userId) {
            logger().warn("Unauthorized reply modify attempt: replyId: {}, userId: {}, replyOwnerId: {}", replyId, userId, reply.userId)
            throw GlobalException(DefaultErrorCode.FORBIDDEN)
        }
        
        val replyCount = postReplyRepository.countByParentIdAndUseYnTrue(replyId)
        if (replyCount > 0) {
            logger().warn("Cannot modify reply with children: replyId={}", replyId)
            throw GlobalException(DefaultErrorCode.REPLY_EXISTS)
        }

        val post = postRepository.findByIdAndUseYnTrue(postId)
            ?: run {
                logger().warn("Post not found for reply modify: postId={}", postId)
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "게시글을 찾을 수 없습니다.")
            }
        
        val authorNickname = memberService.getMemberNickname(loginId)
        val entity = request.toEntity(post.boardId, postId, replyId, userId, authorNickname)
        
        request.fileIds?.forEach { fileId ->
            postFileRepository.updateAttachStatus(
                refType = PostReferenceType.REPLY.ordinal,
                refId = entity.id,
                id = fileId,
                userId = entity.userId
            )
        }

        val savedEntity = postReplyRepository.save(entity)
        return PostReplyResponse.of(savedEntity)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun remove(postId: Long, replyId: Long, userId: Long) {
        val replyCount = postReplyRepository.countByParentIdAndUseYnTrue(replyId)

        if (replyCount > 0) {
            logger().warn("Cannot delete reply with children: replyId={}", replyId)
            throw GlobalException(DefaultErrorCode.REPLY_EXISTS)
        }

        val reply = postReplyRepository.findByIdAndUseYnTrue(replyId)
            ?: run {
                logger().warn("Reply not found for delete: replyId={}", replyId)
                throw GlobalException(DefaultErrorCode.REPLY_NOT_FOUND)
            }
        
        if (reply.userId != userId) {
            logger().warn("Unauthorized reply delete attempt: replyId: {}, userId: {}, replyOwnerId: {}", replyId, userId, reply.userId)
            throw GlobalException(DefaultErrorCode.FORBIDDEN)
        }
        
        reply.softDelete()
        postReplyRepository.save(reply)
        postRepository.increaseReplyCount(postId)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun increaseCount(postId: Long, replyId: Long, userId: Long, countType: CountType): Int {
        val post = postRepository.findByIdAndUseYnTrue(postId)
            ?: run {
                logger().warn("Post not found for reply count increase: postId={}", postId)
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "게시글을 찾을 수 없습니다.")
            }
        val existCount = postCountHistoryRepository.countByRefTypeAndRefIdAndUserId(
            PostReferenceType.REPLY, replyId, userId
        )

        if (existCount > 0) {
            logger().warn("Already exists count history for replyId: {}, userId: {}", replyId, userId)
            throw GlobalException(DefaultErrorCode.ALREADY_EXISTS)
        }

        val updateResult = when (countType) {
            CountType.LIKE -> postReplyRepository.increaseLikeCount(replyId)
            CountType.DISLIKE -> {
                logger().warn("DISLIKE count type is not supported for replyId: {}", replyId)
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "싫어요 기능은 PostLikeEntity로 관리되지 않습니다.")
            }
            else -> {
                logger().error("Invalid count type: {} for replyId: {}", countType, replyId)
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
            }
        }

        if (updateResult <= 0) {
            logger().error("Failed to increase count for replyId: {}, countType: {}", replyId, countType)
            throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
        }

        val history = PostCountHistoryEntity(
            refType = PostReferenceType.REPLY,
            refId = replyId,
            userId = userId,
            type = countType
        )
        postCountHistoryRepository.save(history)

        val updatedReply = postReplyRepository.findById(replyId)
            .orElseGet {
                logger().error("Reply not found after update: replyId={}", replyId)
                throw GlobalException(DefaultErrorCode.REPLY_NOT_FOUND)
            }

        return when (countType) {
            CountType.LIKE -> updatedReply.likeCnt
            CountType.DISLIKE -> {
                logger().error("DISLIKE count type should not reach here for replyId: {}", replyId)
                updatedReply.dislikeCnt
            }
            else -> {
                logger().error("Invalid count type in return: {} for replyId: {}", countType, replyId)
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
            }
        }
    }
}