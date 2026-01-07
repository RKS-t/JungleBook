package org.example.junglebook.repository.post

import org.example.junglebook.entity.post.PostReplyEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PostReplyRepository : JpaRepository<PostReplyEntity, Long> {

    fun findByIdAndUseYnTrue(id: Long): PostReplyEntity?

    fun findByPostIdAndUseYnTrueOrderByCreatedDtAsc(
        postId: Long,
        pageable: Pageable
    ): List<PostReplyEntity>

    @Query("""
        SELECT r FROM PostReplyEntity r 
        WHERE r.postId = :postId AND r.pid IS NULL AND r.useYn = true 
        ORDER BY r.createdDt ASC
    """)
    fun findTopLevelRepliesByPostId(
        @Param("postId") postId: Long,
        pageable: Pageable
    ): List<PostReplyEntity>

    @Query("""
        SELECT r FROM PostReplyEntity r 
        WHERE r.pid = :parentId AND r.useYn = true 
        ORDER BY r.createdDt ASC
    """)
    fun findByParentIdAndUseYnTrueOrderByCreatedDtAsc(
        @Param("parentId") parentId: Long
    ): List<PostReplyEntity>

    @Query("""
        SELECT r FROM PostReplyEntity r 
        WHERE r.userId = :userId AND r.useYn = true 
        ORDER BY r.createdDt DESC
    """)
    fun findByUserIdAndUseYnTrueOrderByCreatedDtDesc(
        @Param("userId") userId: Long,
        pageable: Pageable
    ): List<PostReplyEntity>

    @Query("""
        SELECT r FROM PostReplyEntity r 
        WHERE r.postId = :postId AND r.useYn = true 
        ORDER BY r.likeCnt DESC, r.createdDt DESC
    """)
    fun findPopularRepliesByPostId(
        @Param("postId") postId: Long,
        pageable: Pageable
    ): List<PostReplyEntity>

    fun countByPostIdAndUseYnTrue(postId: Long): Long

    @Query("SELECT COUNT(r) FROM PostReplyEntity r WHERE r.pid = :parentId AND r.useYn = true")
    fun countByParentIdAndUseYnTrue(@Param("parentId") parentId: Long): Long

    fun countByUserIdAndUseYnTrue(userId: Long): Long

    @Modifying
    @Query("UPDATE PostReplyEntity r SET r.likeCnt = r.likeCnt + 1, r.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE r.id = :id")
    fun increaseLikeCount(@Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE PostReplyEntity r SET r.likeCnt = GREATEST(0, r.likeCnt - 1), r.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE r.id = :id")
    fun decreaseLikeCount(@Param("id") id: Long): Int

    @Query("""
        SELECT r FROM PostReplyEntity r 
        WHERE r.postId = :postId AND r.useYn = true 
        ORDER BY CASE WHEN r.pid IS NULL THEN r.id ELSE r.pid END, r.createdDt ASC
    """)
    fun findByPostIdWithHierarchy(@Param("postId") postId: Long): List<PostReplyEntity>
}