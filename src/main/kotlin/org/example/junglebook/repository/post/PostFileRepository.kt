package org.example.junglebook.repository.post

import org.example.junglebook.entity.post.PostFileEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PostFileRepository : JpaRepository<PostFileEntity, Long> {

    fun findByRefTypeAndRefId(refType: Int?, refId: Long?): List<PostFileEntity>

    @Modifying
    @Query("UPDATE PostFileEntity f SET f.attachYn = true " +
            "WHERE f.refType = :refType AND f.refId = :refId AND f.id = :id AND f.userId = :userId")
    fun updateAttachStatus(
        @Param("refType") refType: Int?,
        @Param("refId") refId: Long?,
        @Param("id") id: Long,
        @Param("userId") userId: Long?
    ): Int

    @Modifying
    @Query("DELETE FROM PostFileEntity f WHERE f.id = :id AND f.userId = :userId")
    fun deleteByIdAndUserId(@Param("id") id: Long, @Param("userId") userId: Long?): Int

    fun findByUserId(userId: Long?): List<PostFileEntity>

    fun findByAttachYnFalse(): List<PostFileEntity>
}