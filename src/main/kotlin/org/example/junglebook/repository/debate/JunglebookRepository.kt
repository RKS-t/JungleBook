package org.example.junglebook.repository.debate

import org.example.junglebook.entity.debate.JunglebookEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface JunglebookRepository : JpaRepository<JunglebookEntity, Long> {

    @Query("SELECT j FROM JunglebookEntity j WHERE j.hotYn = 1 ORDER BY j.id DESC LIMIT 1")
    fun findHotOne(): JunglebookEntity?

    @Query("SELECT j FROM JunglebookEntity j WHERE j.useYn = 1 ORDER BY j.createdDt DESC")
    fun findInProgressList(): List<JunglebookEntity>

    override fun count(): Long

    @Query("SELECT j FROM JunglebookEntity j ORDER BY j.createdDt DESC")
    fun findPageableList(pageable: Pageable): List<JunglebookEntity>

    override fun findById(id: Long): Optional<JunglebookEntity>

    @Modifying
    @Query("UPDATE JunglebookEntity j SET j.postCnt = j.postCnt + 1 WHERE j.id = :id")
    fun increasePostCount(@Param("id") id: Long): Int
}