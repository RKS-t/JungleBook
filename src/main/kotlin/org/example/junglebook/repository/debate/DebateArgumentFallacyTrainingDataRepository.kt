package org.example.junglebook.repository.debate

import org.example.junglebook.entity.debate.DebateArgumentFallacyTrainingDataEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DebateArgumentFallacyTrainingDataRepository : JpaRepository<DebateArgumentFallacyTrainingDataEntity, Long> {

    fun findByUsedForTraining(usedForTraining: Boolean): List<DebateArgumentFallacyTrainingDataEntity>

    fun findByLabel(label: String): List<DebateArgumentFallacyTrainingDataEntity>

    @Query("""
        SELECT t FROM DebateArgumentFallacyTrainingDataEntity t 
        WHERE t.usedForTraining = false 
        ORDER BY t.createdAt DESC
    """)
    fun findUnusedTrainingData(): List<DebateArgumentFallacyTrainingDataEntity>

    @Query("""
        SELECT COUNT(t) FROM DebateArgumentFallacyTrainingDataEntity t 
        WHERE t.usedForTraining = false
    """)
    fun countUnusedTrainingData(): Long

    @Query("""
        SELECT t FROM DebateArgumentFallacyTrainingDataEntity t 
        WHERE t.usedForTraining = false 
        AND t.label = :label
        ORDER BY t.createdAt DESC
    """)
    fun findUnusedTrainingDataByLabel(@Param("label") label: String): List<DebateArgumentFallacyTrainingDataEntity>

    fun findByArgumentId(argumentId: Long?): List<DebateArgumentFallacyTrainingDataEntity>
}

