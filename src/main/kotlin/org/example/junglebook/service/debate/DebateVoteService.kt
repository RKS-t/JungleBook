package org.example.junglebook.service.debate

import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.entity.debate.DebateVoteEntity
import org.example.junglebook.enums.VoteType
import org.example.junglebook.repository.MemberRepository
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateVoteRepository
import org.example.junglebook.util.logger
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
        val member = memberRepository.findById(memberId).orElse(null)
            ?: run {
                logger().warn("Member not found for vote: memberId: {}", memberId)
                throw GlobalException(DefaultErrorCode.USER_NOT_FOUND)
            }

        val argument = debateArgumentRepository.findById(argumentId).orElse(null)
            ?: run {
                logger().warn("Argument not found for vote: argumentId: {}", argumentId)
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
            }

        val existingVote = debateVoteRepository.findByMemberIdAndArgumentIdAndVoteType(memberId, argumentId, voteType)
        if (existingVote != null) {
            logger().warn("Vote already exists: argumentId: {}, memberId: {}, voteType: {}", argumentId, memberId, voteType)
            throw GlobalException(DefaultErrorCode.ALREADY_EXISTS)
        }

        val newVote = DebateVoteEntity(
            memberId = memberId,
            argumentId = argumentId,
            replyId = null,
            voteType = voteType
        )
        debateVoteRepository.save(newVote)

        val updateResult = when (voteType) {
            VoteType.UPVOTE -> debateArgumentRepository.increaseSupportCount(argumentId)
            VoteType.DOWNVOTE -> debateArgumentRepository.increaseOpposeCount(argumentId)
        }

        return updateResult > 0
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun cancelVote(argumentId: Long, memberId: Long, voteType: VoteType): Boolean {
        val member = memberRepository.findById(memberId).orElse(null)
            ?: run {
                logger().warn("Member not found for cancel vote: memberId: {}", memberId)
                throw GlobalException(DefaultErrorCode.USER_NOT_FOUND)
            }

        val argument = debateArgumentRepository.findById(argumentId).orElse(null)
            ?: run {
                logger().warn("Argument not found for cancel vote: argumentId: {}", argumentId)
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
            }

        val existingVote = debateVoteRepository.findByMemberIdAndArgumentIdAndVoteType(memberId, argumentId, voteType)
            ?: run {
                logger().warn("Vote not found for cancel: argumentId: {}, memberId: {}, voteType: {}", argumentId, memberId, voteType)
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
            }

        debateVoteRepository.delete(existingVote)

        val updateResult = when (voteType) {
            VoteType.UPVOTE -> debateArgumentRepository.decreaseSupportCount(argumentId)
            VoteType.DOWNVOTE -> debateArgumentRepository.decreaseOpposeCount(argumentId)
        }

        return updateResult > 0
    }
}