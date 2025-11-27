package org.example.junglebook.repository.debate

import io.lettuce.core.dynamic.annotation.Param
import org.example.junglebook.entity.MemberEntity
import org.example.junglebook.entity.debate.DebateArgumentEntity
import org.example.junglebook.entity.debate.DebateVoteEntity
import org.example.junglebook.enums.VoteType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface DebateVoteRepository : JpaRepository<DebateVoteEntity, Long> {

    // 주장에 대한 특정 회원의 특정 타입 투표 조회
    fun findByMemberIdAndArgumentIdAndVoteType(memberId: Long, argumentId: Long, voteType: VoteType): DebateVoteEntity?

    // 댓글에 대한 특정 회원의 특정 타입 투표 조회
    fun findByMemberIdAndReplyIdAndVoteType(memberId: Long, replyId: Long, voteType: VoteType): DebateVoteEntity?

    // 주장에 대한 특정 회원의 모든 투표 조회
    fun findByMemberIdAndArgumentId(memberId: Long, argumentId: Long): List<DebateVoteEntity>

    // 댓글에 대한 특정 회원의 모든 투표 조회
    fun findByMemberIdAndReplyId(memberId: Long, replyId: Long): List<DebateVoteEntity>

    // 특정 주장에 대한 투표 타입별 개수 조회
    @Query("SELECT v.voteType, COUNT(v) FROM DebateVoteEntity v WHERE v.argumentId = :argumentId GROUP BY v.voteType")
    fun countByArgumentIdGroupByVoteType(@Param("argumentId") argumentId: Long): List<Array<Any>>

    // 특정 댓글에 대한 투표 타입별 개수 조회
    @Query("SELECT v.voteType, COUNT(v) FROM DebateVoteEntity v WHERE v.replyId = :replyId GROUP BY v.voteType")
    fun countByReplyIdGroupByVoteType(@Param("replyId") replyId: Long): List<Array<Any>>

    // 특정 회원이 특정 주장에 투표했는지 확인
    fun existsByMemberIdAndArgumentId(memberId: Long, argumentId: Long): Boolean

    // 특정 회원이 특정 댓글에 투표했는지 확인
    fun existsByMemberIdAndReplyId(memberId: Long, replyId: Long): Boolean

    // 특정 회원의 모든 투표 조회 (최신순)
    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long): List<DebateVoteEntity>

    // 특정 주장에 대한 모든 투표 조회
    fun findByArgumentIdOrderByCreatedAtDesc(argumentId: Long): List<DebateVoteEntity>

    // 특정 댓글에 대한 모든 투표 조회
    fun findByReplyIdOrderByCreatedAtDesc(replyId: Long): List<DebateVoteEntity>

    // 중복 투표 방지를 위한 복합 조회 (주장)
    fun findByMemberIdAndArgumentIdAndVoteTypeIn(
        memberId: Long,
        argumentId: Long,
        voteTypes: List<VoteType>
    ): List<DebateVoteEntity>

    // 중복 투표 방지를 위한 복합 조회 (댓글)
    fun findByMemberIdAndReplyIdAndVoteTypeIn(
        memberId: Long,
        replyId: Long,
        voteTypes: List<VoteType>
    ): List<DebateVoteEntity>

    // 특정 주장의 지지 투표 수
    fun countByArgumentIdAndVoteType(argumentId: Long, voteType: VoteType): Long

    // 특정 댓글의 지지 투표 수
    fun countByReplyIdAndVoteType(replyId: Long, voteType: VoteType): Long

    // 특정 회원이 주장에 한 투표 삭제
    fun deleteByMemberIdAndArgumentIdAndVoteType(memberId: Long, argumentId: Long, voteType: VoteType): Int

    // 특정 회원이 댓글에 한 투표 삭제
    fun deleteByMemberIdAndReplyIdAndVoteType(memberId: Long, replyId: Long, voteType: VoteType): Int
}