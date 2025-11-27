package org.example.junglebook.repository.debate


import org.example.junglebook.entity.debate.DebateReplyEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface DebateReplyRepository : JpaRepository<DebateReplyEntity, Long> {

    // ===== 기본 조회 =====

    fun findByIdAndActiveYnTrue(id: Long): DebateReplyEntity?

    // ===== 논증별 댓글 조회 =====

    // 논증의 전체 댓글 조회 (최신순)
    @Query("""
        SELECT r FROM DebateReplyEntity r 
        WHERE r.argumentId = :argumentId AND r.activeYn = true 
        ORDER BY r.createdAt DESC
    """)
    fun findByArgumentIdAndActiveYnTrueOrderByCreatedAtDesc(
        @Param("argumentId") argumentId: Long,
        pageable: Pageable
    ): List<DebateReplyEntity>

    // 논증의 최상위 댓글만 조회 (부모 댓글만)
    @Query("""
        SELECT r FROM DebateReplyEntity r 
        WHERE r.argumentId = :argumentId 
        AND r.parentId IS NULL 
        AND r.activeYn = true 
        ORDER BY r.createdAt ASC
    """)
    fun findTopLevelRepliesByArgumentId(
        @Param("argumentId") argumentId: Long,
        pageable: Pageable
    ): List<DebateReplyEntity>

    // 특정 댓글의 대댓글 조회
    @Query("""
        SELECT r FROM DebateReplyEntity r 
        WHERE r.parentId = :parentId AND r.activeYn = true 
        ORDER BY r.createdAt ASC
    """)
    fun findByParentIdAndActiveYnTrueOrderByCreatedAtAsc(
        @Param("parentId") parentId: Long
    ): List<DebateReplyEntity>

    // 논증의 댓글 개수
    fun countByArgumentIdAndActiveYnTrue(argumentId: Long): Long

    // 특정 댓글의 대댓글 개수
    fun countByParentIdAndActiveYnTrue(parentId: Long): Long

    // ===== 작성자별 조회 =====

    @Query("""
        SELECT r FROM DebateReplyEntity r 
        WHERE r.userId = :userId AND r.activeYn = true 
        ORDER BY r.createdAt DESC
    """)
    fun findByUserIdAndActiveYnTrueOrderByCreatedAtDesc(
        @Param("userId") userId: Long,
        pageable: Pageable
    ): List<DebateReplyEntity>

    fun countByUserIdAndActiveYnTrue(userId: Long): Long

    // ===== 인기 댓글 조회 =====

    @Query("""
        SELECT r FROM DebateReplyEntity r 
        WHERE r.argumentId = :argumentId AND r.activeYn = true 
        ORDER BY (r.supportCount - r.opposeCount) DESC, r.createdAt DESC
    """)
    fun findPopularRepliesByArgumentId(
        @Param("argumentId") argumentId: Long,
        pageable: Pageable
    ): List<DebateReplyEntity>

    // ===== 업데이트 =====

    // 지지 수 증가
    @Modifying
    @Query("""
        UPDATE DebateReplyEntity r 
        SET r.supportCount = r.supportCount + 1, r.updatedAt = CURRENT_TIMESTAMP 
        WHERE r.id = :id
    """)
    fun increaseSupportCount(@Param("id") id: Long): Int

    // 반대 수 증가
    @Modifying
    @Query("""
        UPDATE DebateReplyEntity r 
        SET r.opposeCount = r.opposeCount + 1, r.updatedAt = CURRENT_TIMESTAMP 
        WHERE r.id = :id
    """)
    fun increaseOpposeCount(@Param("id") id: Long): Int

    // 지지 수 감소
    @Modifying
    @Query("""
        UPDATE DebateReplyEntity r 
        SET r.supportCount = GREATEST(0, r.supportCount - 1), r.updatedAt = CURRENT_TIMESTAMP 
        WHERE r.id = :id
    """)
    fun decreaseSupportCount(@Param("id") id: Long): Int

    // 반대 수 감소
    @Modifying
    @Query("""
        UPDATE DebateReplyEntity r 
        SET r.opposeCount = GREATEST(0, r.opposeCount - 1), r.updatedAt = CURRENT_TIMESTAMP 
        WHERE r.id = :id
    """)
    fun decreaseOpposeCount(@Param("id") id: Long): Int

    // ===== 삭제 =====

    // 소프트 삭제
    @Modifying
    @Query("""
        UPDATE DebateReplyEntity r 
        SET r.activeYn = false, r.updatedAt = CURRENT_TIMESTAMP 
        WHERE r.id = :id AND r.userId = :userId
    """)
    fun softDelete(@Param("id") id: Long, @Param("userId") userId: Long): Int

    // 특정 논증의 모든 댓글 삭제 (논증 삭제 시 사용)
    @Modifying
    @Query("""
        UPDATE DebateReplyEntity r 
        SET r.activeYn = false, r.updatedAt = CURRENT_TIMESTAMP 
        WHERE r.argumentId = :argumentId
    """)
    fun softDeleteAllByArgumentId(@Param("argumentId") argumentId: Long): Int

    // 특정 댓글과 모든 대댓글 삭제
    @Modifying
    @Query("""
        UPDATE DebateReplyEntity r 
        SET r.activeYn = false, r.updatedAt = CURRENT_TIMESTAMP 
        WHERE r.id = :id OR r.parentId = :id
    """)
    fun softDeleteWithChildren(@Param("id") id: Long): Int

    // ===== 통계 =====

    // 특정 기간 내 댓글 수
    fun countByArgumentIdAndActiveYnTrueAndCreatedAtBetween(
        argumentId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Long
}