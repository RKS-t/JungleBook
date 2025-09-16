package org.example.junglebook.repository.debate

import org.example.junglebook.entity.debate.DebateArgumentEntity
import org.example.junglebook.enums.ArgumentStance
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DebateArgumentRepository : JpaRepository<DebateArgumentEntity, Long> {

    // 입장별 인기 주장 조회
    @Query("SELECT a FROM DebateArgumentEntity a WHERE a.topicId = :topicId AND a.stance = :stance " +
            "AND a.activeYn = true ORDER BY a.supportCount DESC, a.viewCount DESC")
    fun findPopularByStance(@Param("topicId") topicId: Long, @Param("stance") stance: ArgumentStance): List<DebateArgumentEntity>

    // 입장별 최신 주장 페이징 조회
    @Query("SELECT a FROM DebateArgumentEntity a WHERE a.topicId = :topicId AND a.stance = :stance " +
            "AND a.activeYn = true ORDER BY a.createdAt DESC")
    fun findByTopicIdAndStanceAndActiveYnTrueOrderByCreatedAtDesc(
        @Param("topicId") topicId: Long,
        @Param("stance") stance: ArgumentStance,
        pageable: Pageable
    ): List<DebateArgumentEntity>

    // 입장별 주장 개수 조회
    @Query("SELECT COUNT(a) FROM DebateArgumentEntity a WHERE a.topicId = :topicId AND a.stance = :stance AND a.activeYn = true")
    fun countByTopicIdAndStanceAndActiveYnTrue(@Param("topicId") topicId: Long, @Param("stance") stance: ArgumentStance): Int

    // 특정 주장 조회
    @Query("SELECT a FROM DebateArgumentEntity a WHERE a.id = :id AND a.topicId = :topicId AND a.activeYn = true")
    fun findByIdAndTopicIdAndActiveYnTrue(@Param("id") id: Long, @Param("topicId") topicId: Long): DebateArgumentEntity?

    // 조회수 증가
    @Modifying
    @Query("UPDATE DebateArgumentEntity a SET a.viewCount = a.viewCount + 1, a.updatedAt = CURRENT_TIMESTAMP WHERE a.id = :id")
    fun increaseViewCount(@Param("id") id: Long): Int

    // 지지 수 증가
    @Modifying
    @Query("UPDATE DebateArgumentEntity a SET a.supportCount = a.supportCount + 1, a.updatedAt = CURRENT_TIMESTAMP WHERE a.id = :id")
    fun increaseSupportCount(@Param("id") id: Long): Int

    // 반대 수 증가
    @Modifying
    @Query("UPDATE DebateArgumentEntity a SET a.opposeCount = a.opposeCount + 1, a.updatedAt = CURRENT_TIMESTAMP WHERE a.id = :id")
    fun increaseOpposeCount(@Param("id") id: Long): Int

    // 지지 수 감소
    @Modifying
    @Query("UPDATE DebateArgumentEntity a SET a.supportCount = a.supportCount - 1, a.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE a.id = :id AND a.supportCount > 0")
    fun decreaseSupportCount(@Param("id") id: Long): Int

    // 반대 수 감소
    @Modifying
    @Query("UPDATE DebateArgumentEntity a SET a.opposeCount = a.opposeCount - 1, a.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE a.id = :id AND a.opposeCount > 0")
    fun decreaseOpposeCount(@Param("id") id: Long): Int

    // 소프트 삭제
    @Modifying
    @Query("UPDATE DebateArgumentEntity a SET a.activeYn = false, a.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE a.id = :id AND a.authorId = :authorId")
    fun softDelete(@Param("id") id: Long, @Param("authorId") authorId: Long): Int

    // 토론 주제의 전체 주장 조회 (최신순)
    @Query("SELECT a FROM DebateArgumentEntity a WHERE a.topicId = :topicId AND a.activeYn = true ORDER BY a.createdAt DESC")
    fun findByTopicIdAndActiveYnTrueOrderByCreatedAtDesc(@Param("topicId") topicId: Long, pageable: Pageable): List<DebateArgumentEntity>

    // 토론 주제의 전체 주장 개수
    fun countByTopicIdAndActiveYnTrue(topicId: Long): Int

    // 작성자별 주장 조회
    @Query("SELECT a FROM DebateArgumentEntity a WHERE a.authorId = :authorId AND a.activeYn = true ORDER BY a.createdAt DESC")
    fun findByAuthorIdAndActiveYnTrueOrderByCreatedAtDesc(@Param("authorId") authorId: Long, pageable: Pageable): List<DebateArgumentEntity>

    // 입장별 통계 조회 (토론 현황 파악용)
    @Query("SELECT a.stance, COUNT(a) FROM DebateArgumentEntity a WHERE a.topicId = :topicId AND a.activeYn = true GROUP BY a.stance")
    fun countByTopicIdGroupByStance(@Param("topicId") topicId: Long): List<Array<Any>>
}