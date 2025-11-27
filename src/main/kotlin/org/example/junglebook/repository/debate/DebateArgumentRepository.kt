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

    // ===== 기본 조회 =====

    // 특정 주장 조회
    @Query("SELECT a FROM DebateArgumentEntity a WHERE a.id = :id AND a.topicId = :topicId AND a.activeYn = true")
    fun findByIdAndTopicIdAndActiveYnTrue(@Param("id") id: Long, @Param("topicId") topicId: Long): DebateArgumentEntity?

    // ===== 입장별 조회 =====

    // 입장별 인기 주장 조회 (토픽 포함)
    @Query("""
        SELECT a FROM DebateArgumentEntity a 
        WHERE a.topicId = :topicId AND a.stance = :stance AND a.activeYn = true 
        ORDER BY a.supportCount DESC, a.viewCount DESC
    """)
    fun findPopularByStance(
        @Param("topicId") topicId: Long,
        @Param("stance") stance: ArgumentStance
    ): List<DebateArgumentEntity>

    // 입장별 최신 주장 페이징 조회
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

    // 입장별 주장 개수 조회
    fun countByTopicIdAndStanceAndActiveYnTrue(topicId: Long, stance: ArgumentStance): Long

    // ===== 토픽별 조회 =====

    // 토론 주제의 전체 주장 조회 (최신순)
    @Query("""
        SELECT a FROM DebateArgumentEntity a 
        WHERE a.topicId = :topicId AND a.activeYn = true 
        ORDER BY a.createdAt DESC
    """)
    fun findByTopicIdAndActiveYnTrueOrderByCreatedAtDesc(
        @Param("topicId") topicId: Long,
        pageable: Pageable
    ): List<DebateArgumentEntity>

    // 토론 주제의 전체 주장 개수
    fun countByTopicIdAndActiveYnTrue(topicId: Long): Long

    // ===== 작성자별 조회 =====

    // 작성자별 주장 조회
    @Query("""
        SELECT a FROM DebateArgumentEntity a 
        WHERE a.userId = :userId AND a.activeYn = true 
        ORDER BY a.createdAt DESC
    """)
    fun findByUserIdAndActiveYnTrueOrderByCreatedAtDesc(
        @Param("userId") userId: Long,
        pageable: Pageable
    ): List<DebateArgumentEntity>

    // 작성자별 주장 개수
    fun countByUserIdAndActiveYnTrue(userId: Long): Long

    // ===== 통계 =====

    // 입장별 통계 조회 (토론 현황 파악용)
    @Query("""
        SELECT a.stance, COUNT(a) 
        FROM DebateArgumentEntity a 
        WHERE a.topicId = :topicId AND a.activeYn = true 
        GROUP BY a.stance
    """)
    fun countByTopicIdGroupByStance(@Param("topicId") topicId: Long): List<Array<Any>>

    // 특정 기간 내 논증 수 카운트
    fun countByTopicIdAndActiveYnTrueAndCreatedAtBetween(
        topicId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Long

    // ===== 업데이트 =====

    // 조회수 증가
    @Modifying
    @Query("""
        UPDATE DebateArgumentEntity a 
        SET a.viewCount = a.viewCount + 1, a.updatedAt = CURRENT_TIMESTAMP 
        WHERE a.id = :id
    """)
    fun increaseViewCount(@Param("id") id: Long): Int

    // 지지 수 증가
    @Modifying
    @Query("""
        UPDATE DebateArgumentEntity a 
        SET a.supportCount = a.supportCount + 1, a.updatedAt = CURRENT_TIMESTAMP 
        WHERE a.id = :id
    """)
    fun increaseSupportCount(@Param("id") id: Long): Int

    // 반대 수 증가
    @Modifying
    @Query("""
        UPDATE DebateArgumentEntity a 
        SET a.opposeCount = a.opposeCount + 1, a.updatedAt = CURRENT_TIMESTAMP 
        WHERE a.id = :id
    """)
    fun increaseOpposeCount(@Param("id") id: Long): Int

    // 지지 수 감소
    @Modifying
    @Query("""
        UPDATE DebateArgumentEntity a 
        SET a.supportCount = GREATEST(0, a.supportCount - 1), a.updatedAt = CURRENT_TIMESTAMP 
        WHERE a.id = :id
    """)
    fun decreaseSupportCount(@Param("id") id: Long): Int

    // 반대 수 감소
    @Modifying
    @Query("""
        UPDATE DebateArgumentEntity a 
        SET a.opposeCount = GREATEST(0, a.opposeCount - 1), a.updatedAt = CURRENT_TIMESTAMP 
        WHERE a.id = :id
    """)
    fun decreaseOpposeCount(@Param("id") id: Long): Int

    // ===== 삭제 =====

    // 소프트 삭제
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