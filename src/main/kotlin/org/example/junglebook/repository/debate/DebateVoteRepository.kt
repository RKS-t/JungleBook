package org.example.junglebook.repository.debate

import org.example.junglebook.entity.debate.DebateVoteEntity
import org.example.junglebook.enums.VoteType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DebateVoteRepository : JpaRepository<DebateVoteEntity, Long> {

    fun findByMemberIdAndArgumentIdAndVoteType(memberId: Long, argumentId: Long, voteType: VoteType): DebateVoteEntity?

    fun findByMemberIdAndReplyIdAndVoteType(memberId: Long, replyId: Long, voteType: VoteType): DebateVoteEntity?

    fun findByMemberIdAndArgumentId(memberId: Long, argumentId: Long): List<DebateVoteEntity>

    fun findByMemberIdAndReplyId(memberId: Long, replyId: Long): List<DebateVoteEntity>

    @Query("SELECT v.voteType, COUNT(v) FROM DebateVoteEntity v WHERE v.argumentId = :argumentId GROUP BY v.voteType")
    fun countByArgumentIdGroupByVoteType(@Param("argumentId") argumentId: Long): List<Array<Any>>

    @Query("SELECT v.voteType, COUNT(v) FROM DebateVoteEntity v WHERE v.replyId = :replyId GROUP BY v.voteType")
    fun countByReplyIdGroupByVoteType(@Param("replyId") replyId: Long): List<Array<Any>>

    fun existsByMemberIdAndArgumentId(memberId: Long, argumentId: Long): Boolean

    fun existsByMemberIdAndReplyId(memberId: Long, replyId: Long): Boolean

    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long): List<DebateVoteEntity>

    fun findByArgumentIdOrderByCreatedAtDesc(argumentId: Long): List<DebateVoteEntity>

    fun findByReplyIdOrderByCreatedAtDesc(replyId: Long): List<DebateVoteEntity>

    fun findByMemberIdAndArgumentIdAndVoteTypeIn(
        memberId: Long,
        argumentId: Long,
        voteTypes: List<VoteType>
    ): List<DebateVoteEntity>

    fun findByMemberIdAndReplyIdAndVoteTypeIn(
        memberId: Long,
        replyId: Long,
        voteTypes: List<VoteType>
    ): List<DebateVoteEntity>

    fun countByArgumentIdAndVoteType(argumentId: Long, voteType: VoteType): Long

    fun countByReplyIdAndVoteType(replyId: Long, voteType: VoteType): Long

    fun deleteByMemberIdAndArgumentIdAndVoteType(memberId: Long, argumentId: Long, voteType: VoteType): Int

    fun deleteByMemberIdAndReplyIdAndVoteType(memberId: Long, replyId: Long, voteType: VoteType): Int
}