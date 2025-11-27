package org.example.junglebook.service.debate

import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.entity.debate.DebateReplyEntity
import org.example.junglebook.enums.DebateReferenceType
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateFileRepository
import org.example.junglebook.repository.debate.DebateReplyRepository
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

    /**
     * 1. 댓글 생성
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun createReply(entity: DebateReplyEntity, fileIds: List<Long>?): DebateReplyResponse {
        // 부모 댓글이 있는 경우 깊이 제한 체크 (최대 깊이 2로 제한)
        val parentId = entity.parentId
        if (parentId != null) {
            val parentReply = debateReplyRepository.findByIdAndActiveYnTrue(parentId)
                ?: throw GlobalException(DefaultErrorCode.REPLY_NOT_FOUND)

            if (parentReply.depth >= 1) {
                throw GlobalException(DefaultErrorCode.REPLY_DEPTH_LIMIT_EXCEEDED)
            }
        }

        // 댓글 저장
        val savedEntity = debateReplyRepository.save(entity)

        // 파일 첨부 처리
        fileIds?.forEach { fileId ->
            debateFileRepository.updateAttachStatus(
                refType = DebateReferenceType.REPLY.value,
                refId = savedEntity.id!!,
                id = fileId,
                userId = savedEntity.userId
            )
        }

        // 논증의 댓글 수 증가
        debateArgumentRepository.increaseReplyCount(entity.argumentId)

        return DebateReplyResponse.of(savedEntity)
    }

    // 토론 댓글은 수정 불가 (토론의 무결성을 위해)

    /**
     * 3. 댓글 삭제 (Soft Delete)
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun deleteReply(replyId: Long, userId: Long, deleteChildren: Boolean = false): Boolean {
        val reply = debateReplyRepository.findByIdAndActiveYnTrue(replyId)
            ?: throw GlobalException(DefaultErrorCode.REPLY_NOT_FOUND)

        // 권한 검증
        if (reply.userId != userId) {
            throw GlobalException(DefaultErrorCode.FORBIDDEN)
        }

        // 삭제 처리
        val result = if (deleteChildren) {
            // 대댓글까지 모두 삭제
            debateReplyRepository.softDeleteWithChildren(replyId)
        } else {
            // 해당 댓글만 삭제
            debateReplyRepository.softDelete(replyId, userId)
        }

        if (result > 0) {
            // 논증의 댓글 수 감소
            debateArgumentRepository.decreaseReplyCount(reply.argumentId)
            return true
        }

        return false
    }

    /**
     * 4. 댓글 상세 조회
     */
    @Transactional(readOnly = true)
    fun getReply(replyId: Long): DebateReplyResponse {
        val reply = debateReplyRepository.findByIdAndActiveYnTrue(replyId)
            ?: throw GlobalException(DefaultErrorCode.REPLY_NOT_FOUND)

        return DebateReplyResponse.of(reply)
    }

    /**
     * 5. 논증의 댓글 목록 (페이징)
     */
    @Transactional(readOnly = true)
    fun getRepliesByArgument(argumentId: Long, pageNo: Int, limit: Int): DebateReplyListResponse {
        val pageable = PageRequest.of(pageNo, limit)
        val totalCount = debateReplyRepository.countByArgumentIdAndActiveYnTrue(argumentId)
        val list = debateReplyRepository.findByArgumentIdAndActiveYnTrueOrderByCreatedAtDesc(argumentId, pageable)

        return DebateReplyListResponse.of(totalCount.toInt(), pageNo, list)
    }

    /**
     * 6. 논증의 최상위 댓글 목록 (계층형 구조)
     */
    @Transactional(readOnly = true)
    fun getTopLevelReplies(argumentId: Long, pageNo: Int, limit: Int): DebateReplyListResponse {
        val pageable = PageRequest.of(pageNo, limit)
        val list = debateReplyRepository.findTopLevelRepliesByArgumentId(argumentId, pageable)
        val totalCount = list.size.toLong() // 간단 구현

        return DebateReplyListResponse.of(totalCount.toInt(), pageNo, list)
    }

    /**
     * 7. 특정 댓글의 대댓글 목록
     */
    @Transactional(readOnly = true)
    fun getChildReplies(parentId: Long): List<DebateReplySimpleResponse> {
        val replies = debateReplyRepository.findByParentIdAndActiveYnTrueOrderByCreatedAtAsc(parentId)
        return DebateReplySimpleResponse.of(replies)
    }

    /**
     * 8. 작성자별 댓글 조회
     */
    @Transactional(readOnly = true)
    fun getRepliesByAuthor(userId: Long, pageNo: Int, limit: Int): DebateReplyListResponse {
        val pageable = PageRequest.of(pageNo, limit)
        val totalCount = debateReplyRepository.countByUserIdAndActiveYnTrue(userId)
        val list = debateReplyRepository.findByUserIdAndActiveYnTrueOrderByCreatedAtDesc(userId, pageable)

        return DebateReplyListResponse.of(totalCount.toInt(), pageNo, list)
    }

    /**
     * 9. 인기 댓글 조회
     */
    @Transactional(readOnly = true)
    fun getPopularReplies(argumentId: Long, limit: Int = 5): List<DebateReplySimpleResponse> {
        val pageable = PageRequest.of(0, limit)
        val replies = debateReplyRepository.findPopularRepliesByArgumentId(argumentId, pageable)
        return DebateReplySimpleResponse.of(replies)
    }

    /**
     * 10. 지지 토글
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun toggleSupport(replyId: Long, increase: Boolean): Boolean {
        val result = if (increase) {
            debateReplyRepository.increaseSupportCount(replyId)
        } else {
            debateReplyRepository.decreaseSupportCount(replyId)
        }
        return result > 0
    }

    /**
     * 11. 반대 토글
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun toggleOppose(replyId: Long, increase: Boolean): Boolean {
        val result = if (increase) {
            debateReplyRepository.increaseOpposeCount(replyId)
        } else {
            debateReplyRepository.decreaseOpposeCount(replyId)
        }
        return result > 0
    }

    /**
     * 12. 댓글 통계
     */
    @Transactional(readOnly = true)
    fun getReplyStatistics(argumentId: Long): ReplyStatistics {
        val totalCount = debateReplyRepository.countByArgumentIdAndActiveYnTrue(argumentId)

        val weekAgo = java.time.LocalDateTime.now().minusDays(7)
        val recentCount = debateReplyRepository.countByArgumentIdAndActiveYnTrueAndCreatedAtBetween(
            argumentId, weekAgo, java.time.LocalDateTime.now()
        )

        return ReplyStatistics(
            totalReplies = totalCount.toInt(),
            recentWeeklyReplies = recentCount.toInt()
        )
    }
}