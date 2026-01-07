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

    fun findByIdAndActiveYnTrue(id: Long): DebateReplyEntity?

    @Query("""
        SELECT r FROM DebateReplyEntity r 
        WHERE r.argumentId = :argumentId AND r.activeYn = true 
        ORDER BY r.createdAt DESC
    """)
    fun findByArgumentIdAndActiveYnTrueOrderByCreatedAtDesc(
        @Param("argumentId") argumentId: Long,
        pageable: Pageable
    ): List<DebateReplyEntity>

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

    @Query("""
        SELECT r FROM DebateReplyEntity r 
        WHERE r.parentId = :parentId AND r.activeYn = true 
        ORDER BY r.createdAt ASC
    """)
    fun findByParentIdAndActiveYnTrueOrderByCreatedAtAsc(
        @Param("parentId") parentId: Long
    ): List<DebateReplyEntity>

    fun countByArgumentIdAndActiveYnTrue(argumentId: Long): Long

    fun countByParentIdAndActiveYnTrue(parentId: Long): Long


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

    @Query("""
        SELECT r FROM DebateReplyEntity r 
        WHERE r.argumentId = :argumentId AND r.activeYn = true 
        ORDER BY (r.supportCount - r.opposeCount) DESC, r.createdAt DESC
    """)
    fun findPopularRepliesByArgumentId(
        @Param("argumentId") argumentId: Long,
        pageable: Pageable
    ): List<DebateReplyEntity>

    @Modifying
    @Query("""
        UPDATE DebateReplyEntity r 
        SET r.supportCount = r.supportCount + 1, r.updatedAt = CURRENT_TIMESTAMP 
        WHERE r.id = :id
    """)
    fun increaseSupportCount(@Param("id") id: Long): Int

    @Modifying
    @Query("""
        UPDATE DebateReplyEntity r 
        SET r.opposeCount = r.opposeCount + 1, r.updatedAt = CURRENT_TIMESTAMP 
        WHERE r.id = :id
    """)
    fun increaseOpposeCount(@Param("id") id: Long): Int

    @Modifying
    @Query("""
        UPDATE DebateReplyEntity r 
        SET r.supportCount = GREATEST(0, r.supportCount - 1), r.updatedAt = CURRENT_TIMESTAMP 
        WHERE r.id = :id
    """)
    fun decreaseSupportCount(@Param("id") id: Long): Int

    @Modifying
    @Query("""
        UPDATE DebateReplyEntity r 
        SET r.opposeCount = GREATEST(0, r.opposeCount - 1), r.updatedAt = CURRENT_TIMESTAMP 
        WHERE r.id = :id
    """)
    fun decreaseOpposeCount(@Param("id") id: Long): Int

    @Modifying
    @Query("""
        UPDATE DebateReplyEntity r 
        SET r.activeYn = false, r.updatedAt = CURRENT_TIMESTAMP 
        WHERE r.id = :id AND r.userId = :userId
    """)
    fun softDelete(@Param("id") id: Long, @Param("userId") userId: Long): Int

    @Modifying
    @Query("""
        UPDATE DebateReplyEntity r 
        SET r.activeYn = false, r.updatedAt = CURRENT_TIMESTAMP 
        WHERE r.argumentId = :argumentId
    """)
    fun softDeleteAllByArgumentId(@Param("argumentId") argumentId: Long): Int

    @Modifying
    @Query("""
        UPDATE DebateReplyEntity r 
        SET r.activeYn = false, r.updatedAt = CURRENT_TIMESTAMP 
        WHERE r.id = :id OR r.parentId = :id
    """)
    fun softDeleteWithChildren(@Param("id") id: Long): Int

    fun countByArgumentIdAndActiveYnTrueAndCreatedAtBetween(
        argumentId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Long
}