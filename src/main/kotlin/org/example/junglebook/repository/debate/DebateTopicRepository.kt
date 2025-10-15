package org.example.junglebook.repository.debate

import org.example.junglebook.entity.debate.DebateTopicEntity
import org.example.junglebook.enums.DebateTopicCategory
import org.example.junglebook.enums.DebateTopicStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional


@Repository
interface DebateTopicRepository : JpaRepository<DebateTopicEntity, Long> {

    // ===== 기본 조회 =====

    fun findByIdAndActiveYnTrue(id: Long): DebateTopicEntity?

    fun findByActiveYnTrueOrderByCreatedAtDesc(pageable: Pageable): Page<DebateTopicEntity>

    // ===== 카테고리별 조회 =====

    fun findByCategoryAndActiveYnTrueOrderByCreatedAtDesc(
        category: DebateTopicCategory,
        pageable: Pageable
    ): Page<DebateTopicEntity>

    // ===== 상태별 조회 =====

    fun findByStatusAndActiveYnTrueOrderByCreatedAtDesc(
        status: DebateTopicStatus,
        pageable: Pageable
    ): Page<DebateTopicEntity>

    // ===== Hot 토픽 조회 =====

    fun findByHotYnTrueAndActiveYnTrueOrderByViewCountDesc(
        pageable: Pageable
    ): Page<DebateTopicEntity>

    // ===== 인기순 조회 (논증 수 + 조회수) =====

    @Query("""
        SELECT t FROM DebateTopicEntity t 
        WHERE t.activeYn = true 
        ORDER BY (t.argumentCount + t.viewCount) DESC, t.createdAt DESC
    """)
    fun findByActiveYnTrueOrderByPopularity(pageable: Pageable): Page<DebateTopicEntity>

    // ===== 조회수순 =====

    fun findByActiveYnTrueOrderByViewCountDesc(pageable: Pageable): Page<DebateTopicEntity>

    // ===== 논증 많은 순 =====

    fun findByActiveYnTrueOrderByArgumentCountDesc(pageable: Pageable): Page<DebateTopicEntity>

    // ===== 마감 임박순 =====

    @Query("""
        SELECT t FROM DebateTopicEntity t 
        WHERE t.activeYn = true 
        AND t.endDate IS NOT NULL 
        AND t.endDate >= :today
        AND t.status = 'ACTIVE'
        ORDER BY t.endDate ASC
    """)
    fun findEndingSoonTopics(
        @Param("today") today: LocalDate,
        pageable: Pageable
    ): Page<DebateTopicEntity>

    // ===== 검색 =====

    @Query("""
        SELECT t FROM DebateTopicEntity t 
        WHERE t.activeYn = true 
        AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) 
             OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY t.createdAt DESC
    """)
    fun searchByKeyword(
        @Param("keyword") keyword: String,
        pageable: Pageable
    ): Page<DebateTopicEntity>

    // ===== 복합 검색 =====

    @Query("""
        SELECT t FROM DebateTopicEntity t 
        WHERE t.activeYn = true 
        AND (:category IS NULL OR t.category = :category)
        AND (:status IS NULL OR t.status = :status)
        AND (:keyword IS NULL OR 
             LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
             LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:hotOnly = false OR t.hotYn = true)
        ORDER BY t.createdAt DESC
    """)
    fun searchWithFilters(
        @Param("category") category: DebateTopicCategory?,
        @Param("status") status: DebateTopicStatus?,
        @Param("keyword") keyword: String?,
        @Param("hotOnly") hotOnly: Boolean,
        pageable: Pageable
    ): Page<DebateTopicEntity>

    // ===== 특정 작성자의 토픽 =====

    fun findByCreatorIdAndActiveYnTrueOrderByCreatedAtDesc(
        creatorId: Long,
        pageable: Pageable
    ): Page<DebateTopicEntity>

    // ===== 진행 중인 토픽 (기간 내) =====

    @Query("""
        SELECT t FROM DebateTopicEntity t 
        WHERE t.activeYn = true 
        AND t.status = 'ACTIVE'
        AND (t.startDate IS NULL OR t.startDate <= :today)
        AND (t.endDate IS NULL OR t.endDate >= :today)
        ORDER BY t.createdAt DESC
    """)
    fun findOngoingTopics(
        @Param("today") today: LocalDate,
        pageable: Pageable
    ): Page<DebateTopicEntity>

    // ===== 특정 기간 내 생성된 토픽 =====

    fun findByActiveYnTrueAndCreatedAtBetweenOrderByCreatedAtDesc(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<DebateTopicEntity>

    // ===== 조회수 증가 =====

    @Modifying
    @Query("UPDATE DebateTopicEntity t SET t.viewCount = t.viewCount + 1 WHERE t.id = :id")
    fun increaseViewCount(@Param("id") id: Long)

    // ===== 논증 수 증가 =====

    @Modifying
    @Query("UPDATE DebateTopicEntity t SET t.argumentCount = t.argumentCount + 1 WHERE t.id = :id")
    fun increaseArgumentCount(@Param("id") id: Long)

    // ===== 논증 수 감소 =====

    @Modifying
    @Query("UPDATE DebateTopicEntity t SET t.argumentCount = GREATEST(0, t.argumentCount - 1) WHERE t.id = :id")
    fun decreaseArgumentCount(@Param("id") id: Long)

    // ===== 통계용 카운트 =====

    fun countByActiveYnTrue(): Long

    fun countByCategoryAndActiveYnTrue(category: DebateTopicCategory): Long

    fun countByStatusAndActiveYnTrue(status: DebateTopicStatus): Long

    fun countByHotYnTrueAndActiveYnTrue(): Long



    @Query("""
        SELECT t.category, COUNT(t) 
        FROM DebateTopicEntity t 
        WHERE t.activeYn = true 
        GROUP BY t.category
    """)
    fun countByCategory(): List<Array<Any>>
}