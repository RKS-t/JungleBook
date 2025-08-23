package org.example.junglebook.repository.debate

import org.example.junglebook.entity.debate.DebateReplyEntity
import org.example.junglebook.entity.debate.JunglebookPostReplyEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


@Repository
interface DebateReplyRepository : JpaRepository<DebateReplyEntity, Long> {

    @Modifying
    @Query("UPDATE DebateReplyEntity r SET r.supportCount = r.supportCount + 1 WHERE r.id = :seqNo AND r.argumentId = :argumentId")
    fun increaseLikeCount(@Param("argumentId") argumentId: Long, @Param("seqNo") seqNo: Long): Int

    @Modifying
    @Query("UPDATE DebateReplyEntity r SET r.opposeCount = r.opposeCount + 1 WHERE r.id = :seqNo AND r.argumentId = :argumentId")
    fun increaseDislikeCount(@Param("argumentId") argumentId: Int, @Param("seqNo") seqNo: Long): Int

    @Modifying
    @Query("UPDATE DebateReplyEntity r SET r.activeYn = false WHERE r.id = :id AND r.authorId = :authorId")
    fun remove(@Param("id") id: Long, @Param("authorId") authorId: Long): Int
}