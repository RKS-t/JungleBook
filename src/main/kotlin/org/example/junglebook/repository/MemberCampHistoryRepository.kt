package org.example.junglebook.repository

import org.example.junglebook.entity.MemberCampHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface MemberCampHistoryRepository : JpaRepository<MemberCampHistoryEntity, Long> {

    // 특정 회원의 가장 최근 이데올로기 변경 히스토리 조회
    @Query("""
        SELECT h FROM MemberCampHistoryEntity h 
        WHERE h.memberId = :memberId 
        ORDER BY h.createdAt DESC 
        LIMIT 1
    """)
    fun findLatestByMemberId(@Param("memberId") memberId: Long): MemberCampHistoryEntity?

    // 특정 회원의 모든 이데올로기 변경 히스토리 조회 (최신순)
    @Query("""
        SELECT h FROM MemberCampHistoryEntity h 
        WHERE h.memberId = :memberId 
        ORDER BY h.createdAt DESC
    """)
    fun findAllByMemberIdOrderByCreatedAtDesc(@Param("memberId") memberId: Long): List<MemberCampHistoryEntity>

    // 특정 회원의 특정 기간 내 이데올로기 변경 히스토리 조회
    @Query("""
        SELECT h FROM MemberCampHistoryEntity h 
        WHERE h.memberId = :memberId 
        AND h.createdAt >= :startDate 
        AND h.createdAt <= :endDate 
        ORDER BY h.createdAt DESC
    """)
    fun findByMemberIdAndDateRange(
        @Param("memberId") memberId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<MemberCampHistoryEntity>

    // 특정 회원이 6개월 이내에 이데올로기를 변경했는지 확인
    @Query("""
        SELECT COUNT(h) > 0 
        FROM MemberCampHistoryEntity h 
        WHERE h.memberId = :memberId 
        AND h.createdAt > :sixMonthsAgo
    """)
    fun hasChangedIdeologyWithinSixMonths(
        @Param("memberId") memberId: Long,
        @Param("sixMonthsAgo") sixMonthsAgo: LocalDateTime
    ): Boolean

    // 특정 이데올로지로 변경한 회원 수 조회
    @Query("""
        SELECT COUNT(h) 
        FROM MemberCampHistoryEntity h 
        WHERE h.camp = :camp
    """)
    fun countByCamp(@Param("camp") camp: Int): Long

    // 전체 이데올로지 변경 통계 조회 (각 이데올로지별 변경 횟수)
    @Query("""
        SELECT h.camp, COUNT(h) 
        FROM MemberCampHistoryEntity h 
        GROUP BY h.camp 
        ORDER BY h.camp
    """)
    fun getIdeologyChangeStatistics(): List<Array<Any>>

    // 특정 기간 동안의 이데올로지 변경 추이
    @Query("""
        SELECT h.camp, COUNT(h) 
        FROM MemberCampHistoryEntity h 
        WHERE h.createdAt >= :startDate 
        AND h.createdAt <= :endDate 
        GROUP BY h.camp 
        ORDER BY h.camp
    """)
    fun getIdeologyChangeTrend(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>
}