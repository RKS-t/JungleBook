package org.example.junglebook.repository.post

import io.lettuce.core.dynamic.annotation.Param
import org.example.junglebook.entity.post.PostFileEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
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

    // 첨부되지 않은 파일들 조회 (정리용)
    fun findByAttachYnFalse(): List<PostFileEntity>
}