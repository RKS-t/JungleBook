package org.example.junglebook.service.debate

import org.example.junglebook.constant.JBConstants
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.entity.debate.DebateReplyEntity
import org.example.junglebook.enums.DebateReferenceType
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateFileRepository
import org.example.junglebook.repository.debate.DebateReplyRepository
import org.example.junglebook.util.logger
import org.example.junglebook.web.dto.DebateReplyListResponse
import org.example.junglebook.web.dto.DebateReplyResponse
import org.example.junglebook.web.dto.DebateReplySimpleResponse
import org.example.junglebook.web.dto.ReplyStatistics
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class DebateReplyService(
    private val debateReplyRepository: DebateReplyRepository,
    private val debateArgumentRepository: DebateArgumentRepository,
    private val debateFileRepository: DebateFileRepository,
) {

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun createReply(entity: DebateReplyEntity, fileIds: List<Long>?): DebateReplyResponse {
        val parentId = entity.parentId
        if (parentId != null) {
            val parentReply = debateReplyRepository.findByIdAndActiveYnTrue(parentId)
                ?: throw GlobalException(DefaultErrorCode.REPLY_NOT_FOUND)

            if (parentReply.depth >= 1) {
                throw GlobalException(DefaultErrorCode.REPLY_DEPTH_LIMIT_EXCEEDED)
            }
        }

        val savedEntity = debateReplyRepository.save(entity)
        val savedEntityId = requireNotNull(savedEntity.id) { "Saved reply ID must not be null" }

        fileIds?.forEach { fileId ->
            debateFileRepository.updateAttachStatus(
                refType = DebateReferenceType.REPLY.value,
                refId = savedEntityId,
                id = fileId,
                userId = savedEntity.userId
            )
        }

        debateArgumentRepository.increaseReplyCount(entity.argumentId)

        return DebateReplyResponse.of(savedEntity)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun deleteReply(replyId: Long, userId: Long, deleteChildren: Boolean = false) {
        val reply = debateReplyRepository.findByIdAndActiveYnTrue(replyId)
            ?: throw GlobalException(DefaultErrorCode.REPLY_NOT_FOUND)

        if (reply.userId != userId) {
            logger().warn("Unauthorized reply delete attempt: replyId: {}, userId: {}, replyOwnerId: {}", replyId, userId, reply.userId)
            throw GlobalException(DefaultErrorCode.FORBIDDEN)
        }

        val result = if (deleteChildren) {
            debateReplyRepository.softDeleteWithChildren(replyId)
        } else {
            debateReplyRepository.softDelete(replyId, userId)
        }

        if (result <= 0) {
            throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
        }

        debateArgumentRepository.decreaseReplyCount(reply.argumentId)
    }

    @Transactional(readOnly = true)
    fun getReply(replyId: Long): DebateReplyResponse {
        val reply = debateReplyRepository.findByIdAndActiveYnTrue(replyId)
            ?: throw GlobalException(DefaultErrorCode.REPLY_NOT_FOUND)

        return DebateReplyResponse.of(reply)
    }

    @Transactional(readOnly = true)
    fun getRepliesByArgument(argumentId: Long, pageNo: Int, limit: Int): DebateReplyListResponse {
        val pageable = PageRequest.of(pageNo, limit)
        val totalCount = debateReplyRepository.countByArgumentIdAndActiveYnTrue(argumentId)
        val list = debateReplyRepository.findByArgumentIdAndActiveYnTrueOrderByCreatedAtDesc(argumentId, pageable)

        return DebateReplyListResponse.of(totalCount.toInt(), pageNo, list)
    }

    @Transactional(readOnly = true)
    fun getTopLevelReplies(argumentId: Long, pageNo: Int, limit: Int): DebateReplyListResponse {
        val pageable = PageRequest.of(pageNo, limit)
        val list = debateReplyRepository.findTopLevelRepliesByArgumentId(argumentId, pageable)
        val totalCount = list.size.toLong()

        return DebateReplyListResponse.of(totalCount.toInt(), pageNo, list)
    }

    @Transactional(readOnly = true)
    fun getChildReplies(parentId: Long): List<DebateReplySimpleResponse> {
        val replies = debateReplyRepository.findByParentIdAndActiveYnTrueOrderByCreatedAtAsc(parentId)
        return DebateReplySimpleResponse.of(replies)
    }

    @Transactional(readOnly = true)
    fun getRepliesByAuthor(userId: Long, pageNo: Int, limit: Int): DebateReplyListResponse {
        val pageable = PageRequest.of(pageNo, limit)
        val totalCount = debateReplyRepository.countByUserIdAndActiveYnTrue(userId)
        val list = debateReplyRepository.findByUserIdAndActiveYnTrueOrderByCreatedAtDesc(userId, pageable)

        return DebateReplyListResponse.of(totalCount.toInt(), pageNo, list)
    }

    @Transactional(readOnly = true)
    fun getPopularReplies(argumentId: Long, limit: Int = JBConstants.DEBATE_DEFAULT_POPULAR_REPLIES_LIMIT): List<DebateReplySimpleResponse> {
        val pageable = PageRequest.of(0, limit)
        val replies = debateReplyRepository.findPopularRepliesByArgumentId(argumentId, pageable)
        return DebateReplySimpleResponse.of(replies)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun toggleSupport(replyId: Long, increase: Boolean): Boolean {
        val result = if (increase) {
            debateReplyRepository.increaseSupportCount(replyId)
        } else {
            debateReplyRepository.decreaseSupportCount(replyId)
        }
        return result > 0
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun toggleOppose(replyId: Long, increase: Boolean): Boolean {
        val result = if (increase) {
            debateReplyRepository.increaseOpposeCount(replyId)
        } else {
            debateReplyRepository.decreaseOpposeCount(replyId)
        }
        return result > 0
    }

    @Transactional(readOnly = true)
    fun getReplyStatistics(argumentId: Long): ReplyStatistics {
        val totalCount = debateReplyRepository.countByArgumentIdAndActiveYnTrue(argumentId)

        val weekAgo = java.time.LocalDateTime.now().minusDays(JBConstants.DEBATE_RECENT_WEEKS.toLong())
        val recentCount = debateReplyRepository.countByArgumentIdAndActiveYnTrueAndCreatedAtBetween(
            argumentId, weekAgo, java.time.LocalDateTime.now()
        )

        return ReplyStatistics(
            totalReplies = totalCount.toInt(),
            recentWeeklyReplies = recentCount.toInt()
        )
    }
}