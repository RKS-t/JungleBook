package org.example.junglebook.repository.debate

import io.lettuce.core.dynamic.annotation.Param
import org.example.junglebook.entity.debate.JunglebookFileEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository


@Repository
interface JunglebookFileRepository : JpaRepository<JunglebookFileEntity, Long> {

    @Modifying
    @Query(
        "UPDATE JunglebookFileEntity f SET f.attachYn = true " +
                "WHERE f.refType = :refType AND f.refId = :refId AND f.id = :id AND f.userId = :userId"
    )
    fun updateAttachStatus(
        @Param("refType") refType: Int,
        @Param("refId") refId: Long?,
        @Param("id") id: Long,
        @Param("userId") userId: Long?
    ): Int

    @Modifying
    @Query("DELETE FROM JunglebookFileEntity f WHERE f.id = :id AND f.userId = :userId")
    fun deleteByIdAndUserId(@Param("id") id: Long, @Param("userId") userId: Long?): Int
}