package org.example.junglebook.service.debate

import org.example.junglebook.constant.JBConstants
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.repository.debate.DebateReplyRepository
import org.example.junglebook.web.dto.DebateReplyListResponse
import org.example.junglebook.web.dto.DebateReplyResponse
import org.example.junglebook.web.dto.DebateReplySimpleResponse
import org.example.junglebook.web.dto.ReplyStatistics
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class DebateReplyQueryService(
    private val debateReplyRepository: DebateReplyRepository
) {

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

    @Transactional(readOnly = true)
    fun getReplyStatistics(argumentId: Long): ReplyStatistics {
        val totalCount = debateReplyRepository.countByArgumentIdAndActiveYnTrue(argumentId)

        val weekAgo = LocalDateTime.now().minusDays(JBConstants.DEBATE_RECENT_WEEKS.toLong())
        val recentCount = debateReplyRepository.countByArgumentIdAndActiveYnTrueAndCreatedAtBetween(
            argumentId, weekAgo, LocalDateTime.now()
        )

        return ReplyStatistics(
            totalReplies = totalCount.toInt(),
            recentWeeklyReplies = recentCount.toInt()
        )
    }
}
