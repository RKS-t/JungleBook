package org.example.junglebook.repository.debate

import org.example.junglebook.entity.debate.DebateFileEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


@Repository
interface DebateFileRepository : JpaRepository<DebateFileEntity, Long> {

    @Modifying
    @Query(
        "UPDATE DebateFileEntity f SET f.activeYn = true " +
                "WHERE f.referenceType = :refType AND f.referenceId = :refId AND f.id = :id AND f.uploaderId = :uploaderId"
    )
    fun updateAttachStatus(
        @Param("refType") refType: Int,
        @Param("refId") refId: Long?,
        @Param("id") id: Long,
        @Param("uploaderId") userId: Long?
    ): Int

    @Modifying
    @Query("DELETE FROM DebateFileEntity f WHERE f.id = :id AND f.uploaderId = :uploaderId")
    fun deleteByIdAndUserId(@Param("id") id: Long, @Param("uploaderId") uploaderId: Long?): Int
}