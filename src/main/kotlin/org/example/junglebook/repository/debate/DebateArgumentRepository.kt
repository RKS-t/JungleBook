package org.example.junglebook.repository.debate

import org.example.junglebook.entity.debate.DebateArgumentEntity
import org.example.junglebook.enums.ArgumentStance
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface DebateArgumentRepository : JpaRepository<DebateArgumentEntity, Long> {

    @Query("SELECT a FROM DebateArgumentEntity a WHERE a.id = :id AND a.topicId = :topicId AND a.activeYn = true")
    fun findByIdAndTopicIdAndActiveYnTrue(@Param("id") id: Long, @Param("topicId") topicId: Long): DebateArgumentEntity?

    @Query("""
        SELECT a FROM DebateArgumentEntity a 
        WHERE a.topicId = :topicId AND a.stance = :stance AND a.activeYn = true 
        ORDER BY a.supportCount DESC, a.viewCount DESC
    """)
    fun findPopularByStance(
        @Param("topicId") topicId: Long,
        @Param("stance") stance: ArgumentStance
    ): List<DebateArgumentEntity>

    @Query("""
        SELECT a FROM DebateArgumentEntity a 
        WHERE a.topicId = :topicId AND a.stance = :stance AND a.activeYn = true 
        ORDER BY a.createdAt DESC
    """)
    fun findByTopicIdAndStanceAndActiveYnTrueOrderByCreatedAtDesc(
        @Param("topicId") topicId: Long,
        @Param("stance") stance: ArgumentStance,
        pageable: Pageable
    ): List<DebateArgumentEntity>

    fun countByTopicIdAndStanceAndActiveYnTrue(topicId: Long, stance: ArgumentStance): Long

    @Query("""
        SELECT a FROM DebateArgumentEntity a 
        WHERE a.topicId = :topicId AND a.activeYn = true 
        ORDER BY a.createdAt DESC
    """)
    fun findByTopicIdAndActiveYnTrueOrderByCreatedAtDesc(
        @Param("topicId") topicId: Long,
        pageable: Pageable
    ): List<DebateArgumentEntity>

    fun countByTopicIdAndActiveYnTrue(topicId: Long): Long

    @Query("""
        SELECT a FROM DebateArgumentEntity a 
        WHERE a.userId = :userId AND a.activeYn = true 
        ORDER BY a.createdAt DESC
    """)
    fun findByUserIdAndActiveYnTrueOrderByCreatedAtDesc(
        @Param("userId") userId: Long,
        pageable: Pageable
    ): List<DebateArgumentEntity>

    fun countByUserIdAndActiveYnTrue(userId: Long): Long

    @Query("""
        SELECT a.stance, COUNT(a) 
        FROM DebateArgumentEntity a 
        WHERE a.topicId = :topicId AND a.activeYn = true 
        GROUP BY a.stance
    """)
    fun countByTopicIdGroupByStance(@Param("topicId") topicId: Long): List<Array<Any>>

    fun countByTopicIdAndActiveYnTrueAndCreatedAtBetween(
        topicId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Long

    @Modifying
    @Query("""
        UPDATE DebateArgumentEntity a 
        SET a.viewCount = a.viewCount + 1, a.updatedAt = CURRENT_TIMESTAMP 
        WHERE a.id = :id
    """)
    fun increaseViewCount(@Param("id") id: Long): Int

    @Modifying
    @Query("""
        UPDATE DebateArgumentEntity a 
        SET a.supportCount = a.supportCount + 1, a.updatedAt = CURRENT_TIMESTAMP 
        WHERE a.id = :id
    """)
    fun increaseSupportCount(@Param("id") id: Long): Int

    @Modifying
    @Query("""
        UPDATE DebateArgumentEntity a 
        SET a.opposeCount = a.opposeCount + 1, a.updatedAt = CURRENT_TIMESTAMP 
        WHERE a.id = :id
    """)
    fun increaseOpposeCount(@Param("id") id: Long): Int

    @Modifying
    @Query("""
        UPDATE DebateArgumentEntity a 
        SET a.supportCount = GREATEST(0, a.supportCount - 1), a.updatedAt = CURRENT_TIMESTAMP 
        WHERE a.id = :id
    """)
    fun decreaseSupportCount(@Param("id") id: Long): Int

    @Modifying
    @Query("""
        UPDATE DebateArgumentEntity a 
        SET a.opposeCount = GREATEST(0, a.opposeCount - 1), a.updatedAt = CURRENT_TIMESTAMP 
        WHERE a.id = :id
    """)
    fun decreaseOpposeCount(@Param("id") id: Long): Int

    @Modifying
    @Query("""
        UPDATE DebateArgumentEntity a 
        SET a.activeYn = false, a.updatedAt = CURRENT_TIMESTAMP 
        WHERE a.id = :id AND a.userId = :userId
    """)
    fun softDelete(@Param("id") id: Long, @Param("userId") userId: Long): Int

    @Modifying
    @Query("UPDATE DebateArgumentEntity a SET a.replyCount = a.replyCount + 1 WHERE a.id = :id")
    fun increaseReplyCount(@Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE DebateArgumentEntity a SET a.replyCount = GREATEST(0, a.replyCount - 1) WHERE a.id = :id")
    fun decreaseReplyCount(@Param("id") id: Long): Int
}