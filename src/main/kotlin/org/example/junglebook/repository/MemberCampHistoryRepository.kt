package org.example.junglebook.repository

import org.example.junglebook.entity.MemberCampHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface MemberCampHistoryRepository : JpaRepository<MemberCampHistoryEntity, Long> {

    @Query("""
        SELECT h FROM MemberCampHistoryEntity h 
        WHERE h.memberId = :memberId 
        ORDER BY h.createdAt DESC
    """)
    fun findLatestByMemberId(@Param("memberId") memberId: Long): List<MemberCampHistoryEntity>

    @Query("""
        SELECT h FROM MemberCampHistoryEntity h 
        WHERE h.memberId = :memberId 
        ORDER BY h.createdAt DESC
    """)
    fun findAllByMemberIdOrderByCreatedAtDesc(@Param("memberId") memberId: Long): List<MemberCampHistoryEntity>

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

    @Query("""
        SELECT COUNT(h) 
        FROM MemberCampHistoryEntity h 
        WHERE h.camp = :camp
    """)
    fun countByCamp(@Param("camp") camp: Int): Long

    @Query("""
        SELECT h.camp, COUNT(h) 
        FROM MemberCampHistoryEntity h 
        GROUP BY h.camp 
        ORDER BY h.camp
    """)
    fun getIdeologyChangeStatistics(): List<Array<Any>>

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