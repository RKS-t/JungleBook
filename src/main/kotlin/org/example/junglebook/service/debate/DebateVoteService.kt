package org.example.junglebook.service.debate

import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.entity.debate.DebateVoteEntity
import org.example.junglebook.enums.VoteType
import org.example.junglebook.repository.MemberRepository
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateVoteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class DebateVoteService (
    private val debateVoteRepository: DebateVoteRepository,
    private val memberRepository: MemberRepository,
    private val debateArgumentRepository: DebateArgumentRepository
){

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun vote(argumentId: Long, memberId: Long, voteType: VoteType): Boolean {
        val member = memberRepository.findById(memberId)
            .orElseThrow { GlobalException(DefaultErrorCode.USER_NOT_FOUND) }

        val argument = debateArgumentRepository.findById(argumentId)
            .orElseThrow { GlobalException(DefaultErrorCode.WRONG_ACCESS) }

        // 동일한 타입의 투표가 이미 있는지만 확인
        val existingVote = debateVoteRepository.findByMemberIdAndArgumentIdAndVoteType(memberId, argumentId, voteType)
        if (existingVote != null) {
            throw GlobalException(DefaultErrorCode.ALREADY_EXISTS)
        }

        // 새 투표 저장 (반대 투표 삭제 로직 제거)
        val newVote = DebateVoteEntity(
            memberId = memberId,
            argumentId = argumentId,
            replyId = null,
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

        val existingVote: DebateVoteEntity = debateVoteRepository.findByMemberIdAndArgumentIdAndVoteType(memberId, argumentId, voteType)
            ?: throw GlobalException(DefaultErrorCode.WRONG_ACCESS)

        debateVoteRepository.delete(existingVote)

        // 카운트 감소
        val updateResult = when (voteType) {
            VoteType.UPVOTE -> debateArgumentRepository.decreaseSupportCount(argumentId)
            VoteType.DOWNVOTE -> debateArgumentRepository.decreaseOpposeCount(argumentId)
        }

        return updateResult > 0
    }
}