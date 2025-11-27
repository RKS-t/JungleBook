package org.example.junglebook.repository.debate

import org.example.junglebook.entity.debate.DebateArgumentFallacyAppealEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DebateArgumentFallacyAppealRepository : JpaRepository<DebateArgumentFallacyAppealEntity, Long> {

    fun findByArgumentId(argumentId: Long): List<DebateArgumentFallacyAppealEntity>

    fun findByArgumentIdAndStatus(argumentId: Long, status: String): List<DebateArgumentFallacyAppealEntity>

    fun countByArgumentId(argumentId: Long): Int

    fun countByArgumentIdAndStatus(argumentId: Long, status: String): Int

    @Query("""
        SELECT a FROM DebateArgumentFallacyAppealEntity a 
        WHERE a.status = :status 
        ORDER BY a.createdAt DESC
    """)
    fun findByStatusOrderByCreatedAtDesc(@Param("status") status: String): List<DebateArgumentFallacyAppealEntity>

    @Query("""
        SELECT a FROM DebateArgumentFallacyAppealEntity a 
        WHERE a.status = 'PENDING' 
        GROUP BY a.argumentId 
        HAVING COUNT(a.id) >= :threshold
        ORDER BY COUNT(a.id) DESC
    """)
    fun findAppealedArgumentsForRetraining(@Param("threshold") threshold: Int): List<DebateArgumentFallacyAppealEntity>
}

