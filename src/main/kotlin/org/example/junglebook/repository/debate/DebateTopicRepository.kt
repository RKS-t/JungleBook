package org.example.junglebook.repository.debate

import org.example.junglebook.entity.debate.DebateTopicEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface DebateTopicRepository : JpaRepository<DebateTopicEntity, Long> {

    @Query("SELECT t FROM DebateTopicEntity t WHERE t.hotYn = true ORDER BY t.id DESC LIMIT 1")
    fun findHotOne(): DebateTopicEntity?

    @Query("SELECT t FROM DebateTopicEntity t WHERE t.status = :status   ORDER BY t.createdAt DESC")
    fun findInStatusList(@Param("status") status: Int): List<DebateTopicEntity>

    override fun count(): Long

    @Query("SELECT t FROM DebateTopicEntity t ORDER BY t.createdAt DESC")
    fun findPageableList(pageable: Pageable): List<DebateTopicEntity>

    override fun findById(id: Long): Optional<DebateTopicEntity>

    @Modifying
    @Query("UPDATE JunglebookEntity t SET t.postCnt = t.postCnt + 1 WHERE t.id = :id")
    fun increasePostCount(@Param("id") id: Long): Int
}