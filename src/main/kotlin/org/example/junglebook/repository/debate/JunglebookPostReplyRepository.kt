package org.example.junglebook.repository.debate

import org.example.junglebook.entity.debate.JunglebookPostReplyEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


@Repository
interface JunglebookPostReplyRepository : JpaRepository<JunglebookPostReplyEntity, Long> {

    fun findByPostIdOrderByCreatedDtAsc(postId: Long): List<JunglebookPostReplyEntity>

    fun countByPostId(postId: Long): Int

    fun countByPostIdAndPid(postId: Long, pid: Long): Int

    @Modifying
    @Query("UPDATE JunglebookPostReplyEntity r SET r.likeCnt = r.likeCnt + 1 WHERE r.id = :seqNo AND r.boardId = :boardId")
    fun increaseLikeCount(@Param("boardId") boardId: Int, @Param("seqNo") seqNo: Long): Int

    @Modifying
    @Query("UPDATE JunglebookPostReplyEntity r SET r.dislikeCnt = r.dislikeCnt + 1 WHERE r.id = :seqNo AND r.boardId = :boardId")
    fun increaseDislikeCount(@Param("boardId") boardId: Int, @Param("seqNo") seqNo: Long): Int

    @Modifying
    @Query("UPDATE JunglebookPostReplyEntity r SET r.useYn = false WHERE r.id = :id AND r.userId = :userId")
    fun softDelete(@Param("id") id: Long, @Param("userId") userId: Long): Int
}