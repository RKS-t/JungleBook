package org.example.junglebook.service.debate

import kr.co.minust.api.exception.DefaultErrorCode
import kr.co.minust.api.exception.GlobalException
import org.example.junglebook.entity.debate.DebateArgumentEntity
import org.example.junglebook.entity.debate.DebateVoteEntity
import org.example.junglebook.enums.ArgumentStance
import org.example.junglebook.enums.DebateReferenceType
import org.example.junglebook.enums.VoteType
import org.example.junglebook.repository.MemberRepository
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateFileRepository
import org.example.junglebook.repository.debate.DebateVoteRepository
import org.example.junglebook.web.dto.DebateArgumentListResponse
import org.example.junglebook.web.dto.DebateArgumentResponse
import org.example.junglebook.web.dto.DebateArgumentSimpleResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional


@Service
class DebateArgumentService(
    private val debateArgumentRepository: DebateArgumentRepository,
    private val debateFileRepository: DebateFileRepository,
    private val debateVoteRepository: DebateVoteRepository,
    private val memberRepository: MemberRepository
) {

    fun popularList(topicId: Long): Map<ArgumentStance, List<DebateArgumentSimpleResponse>> {
        val popular = mutableMapOf<ArgumentStance, List<DebateArgumentSimpleResponse>>()

        ArgumentStance.values().forEach { stance ->
            val arguments = debateArgumentRepository.findPopularByStance(topicId, stance)
            popular[stance] = DebateArgumentSimpleResponse.of(arguments)
        }

        return popular
    }

    fun pageableList(topicId: Long, stance: ArgumentStance, pageNo: Int, limit: Int): DebateArgumentListResponse {
        val totalCount = debateArgumentRepository.countByTopicIdAndStanceAndActiveYnTrue(topicId, stance)
        val pageable = PageRequest.of(pageNo, limit)
        val list = debateArgumentRepository.findByTopicIdAndStanceAndActiveYnTrueOrderByCreatedAtDesc(
            topicId, stance, pageable
        )

        return DebateArgumentListResponse.of(totalCount, pageNo, list)
    }

    fun view(topicId: Long, id: Long): DebateArgumentResponse? {
        val argument = debateArgumentRepository.findByIdAndTopicIdAndActiveYnTrue(id, topicId)
        return argument?.let { DebateArgumentResponse.of(it) }
    }

    fun increaseViewCount(id: Long) {
        debateArgumentRepository.increaseViewCount(id)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun vote(argumentId: Long, memberId: Long, voteType: VoteType): Boolean {
        val member = memberRepository.findById(memberId)
            .orElseThrow { GlobalException(DefaultErrorCode.USER_NOT_FOUND) }

        val argument = debateArgumentRepository.findById(argumentId)
            .orElseThrow { GlobalException(DefaultErrorCode.WRONG_ACCESS) }

        // 동일한 타입의 투표가 이미 있는지만 확인
        val existingVote = debateVoteRepository.findByMemberAndArgumentAndVoteType(member, argument, voteType)
        if (existingVote != null) {
            throw GlobalException(DefaultErrorCode.ALREADY_EXISTS)
        }

        // 새 투표 저장 (반대 투표 삭제 로직 제거)
        val newVote = DebateVoteEntity(
            member = member,
            argument = argument,
            reply = null,
            voteType = voteType
        )
        debateVoteRepository.save(newVote)

        // 카운트 증가
        val updateResult = when (voteType) {
            VoteType.UPVOTE -> debateArgumentRepository.increaseSupportCount(argumentId)
            VoteType.DOWNVOTE -> debateArgumentRepository.increaseOpposeCount(argumentId)
        }

        return updateResult > 0
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun cancelVote(argumentId: Long, memberId: Long, voteType: VoteType): Boolean {
        val member = memberRepository.findById(memberId)
            .orElseThrow { GlobalException(DefaultErrorCode.USER_NOT_FOUND) }

        val argument = debateArgumentRepository.findById(argumentId)
            .orElseThrow { GlobalException(DefaultErrorCode.WRONG_ACCESS) }

        val existingVote = debateVoteRepository.findByMemberAndArgumentAndVoteType(member, argument, voteType)
            ?: throw GlobalException(DefaultErrorCode.WRONG_ACCESS)

        debateVoteRepository.delete(existingVote)

        // 카운트 감소
        val updateResult = when (voteType) {
            VoteType.UPVOTE -> debateArgumentRepository.decreaseSupportCount(argumentId)
            VoteType.DOWNVOTE -> debateArgumentRepository.decreaseOpposeCount(argumentId)
        }

        return updateResult > 0
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun createArgument(entity: DebateArgumentEntity, fileIds: List<Long>?) {
        val savedEntity = debateArgumentRepository.save(entity)

        fileIds?.forEach { fileId ->
            debateFileRepository.updateAttachStatus(
                referenceType = DebateReferenceType.ARGUMENT,
                referenceId = savedEntity.id!!,
                fileId = fileId,
                uploaderId = savedEntity.authorId
            )
        }
    }

    fun getArgumentsByAuthor(authorId: Long, pageNo: Int, limit: Int): DebateArgumentListResponse {
        val pageable = PageRequest.of(pageNo, limit)
        val list = debateArgumentRepository.findByAuthorIdAndActiveYnTrueOrderByCreatedAtDesc(authorId, pageable)
        val totalCount = list.size // 간단한 구현, 실제로는 별도 카운트 쿼리 필요

        return DebateArgumentListResponse.of(totalCount, pageNo, list)
    }

    fun getTopicStatistics(topicId: Long): Map<ArgumentStance, Int> {
        val statistics = debateArgumentRepository.countByTopicIdGroupByStance(topicId)
        return statistics.associate {
            (it[0] as ArgumentStance) to (it[1] as Long).toInt()
        }
    }
}