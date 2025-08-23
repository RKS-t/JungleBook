package org.example.junglebook.repository.post

import org.example.junglebook.entity.post.PostReplyEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

// PostReply Repository
@Repository
interface PostReplyRepository : JpaRepository<PostReplyEntity, Long> {

    fun findByPostIdAndUseYnTrueOrderByCreatedDtAsc(postId: Long): List<PostReplyEntity>

    fun countByPostIdAndUseYnTrue(postId: Long): Int

    fun countByPostIdAndPidAndUseYnTrue(postId: Long, pid: Long): Int

    @Modifying
    @Query("UPDATE PostReplyEntity r SET r.likeCnt = r.likeCnt + 1, r.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE r.id = :id AND r.boardId = :boardId")
    fun increaseLikeCount(@Param("boardId") boardId: Int, @Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE PostReplyEntity r SET r.dislikeCnt = r.dislikeCnt + 1, r.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE r.id = :id AND r.boardId = :boardId")
    fun increaseDislikeCount(@Param("boardId") boardId: Int, @Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE PostReplyEntity r SET r.useYn = false, r.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE r.id = :id AND r.userId = :userId")
    fun softDelete(@Param("id") id: Long, @Param("userId") userId: Long): Int

    // 특정 게시글의 모든 댓글 조회 (대댓글 포함)
    @Query("SELECT r FROM PostReplyEntity r WHERE r.postId = :postId AND r.useYn = true " +
            "ORDER BY CASE WHEN r.pid IS NULL THEN r.id ELSE r.pid END, r.createdDt ASC")
    fun findByPostIdWithHierarchy(postId: Long): List<PostReplyEntity>
}