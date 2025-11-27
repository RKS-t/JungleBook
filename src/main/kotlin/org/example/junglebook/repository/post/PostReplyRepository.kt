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

    // 기본 조회
    fun findByIdAndUseYnTrue(id: Long): PostReplyEntity?

    // 게시글별 댓글 조회
    fun findByPostIdAndUseYnTrueOrderByCreatedDtAsc(
        postId: Long,
        pageable: Pageable
    ): List<PostReplyEntity>

    // 최상위 댓글만 조회 (대댓글 제외)
    @Query("""
        SELECT r FROM PostReplyEntity r 
        WHERE r.postId = :postId AND r.pid IS NULL AND r.useYn = true 
        ORDER BY r.createdDt ASC
    """)
    fun findTopLevelRepliesByPostId(
        @Param("postId") postId: Long,
        pageable: Pageable
    ): List<PostReplyEntity>

    // 특정 댓글의 대댓글 조회
    @Query("""
        SELECT r FROM PostReplyEntity r 
        WHERE r.pid = :parentId AND r.useYn = true 
        ORDER BY r.createdDt ASC
    """)
    fun findByParentIdAndUseYnTrueOrderByCreatedDtAsc(
        @Param("parentId") parentId: Long
    ): List<PostReplyEntity>

    // 작성자별 댓글 조회
    @Query("""
        SELECT r FROM PostReplyEntity r 
        WHERE r.userId = :userId AND r.useYn = true 
        ORDER BY r.createdDt DESC
    """)
    fun findByUserIdAndUseYnTrueOrderByCreatedDtDesc(
        @Param("userId") userId: Long,
        pageable: Pageable
    ): List<PostReplyEntity>

    // 인기 댓글 조회
    @Query("""
        SELECT r FROM PostReplyEntity r 
        WHERE r.postId = :postId AND r.useYn = true 
        ORDER BY r.likeCnt DESC, r.createdDt DESC
    """)
    fun findPopularRepliesByPostId(
        @Param("postId") postId: Long,
        pageable: Pageable
    ): List<PostReplyEntity>

    // 댓글 개수
    fun countByPostIdAndUseYnTrue(postId: Long): Long

    fun countByParentIdAndUseYnTrue(parentId: Long): Long

    fun countByUserIdAndUseYnTrue(userId: Long): Long

    // 좋아요 수 증가/감소
    @Modifying
    @Query("UPDATE PostReplyEntity r SET r.likeCnt = r.likeCnt + 1, r.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE r.id = :id")
    fun increaseLikeCount(@Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE PostReplyEntity r SET r.likeCnt = GREATEST(0, r.likeCnt - 1), r.updatedDt = CURRENT_TIMESTAMP " +
            "WHERE r.id = :id")
    fun decreaseLikeCount(@Param("id") id: Long): Int

    // 특정 게시글의 모든 댓글 조회 (계층형)
    @Query("""
        SELECT r FROM PostReplyEntity r 
        WHERE r.postId = :postId AND r.useYn = true 
        ORDER BY CASE WHEN r.pid IS NULL THEN r.id ELSE r.pid END, r.createdDt ASC
    """)
    fun findByPostIdWithHierarchy(@Param("postId") postId: Long): List<PostReplyEntity>
}