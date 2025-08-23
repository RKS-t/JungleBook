package org.example.junglebook.repository.debate

import org.example.junglebook.entity.debate.JunglebookPostEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface JunglebookPostRepository : JpaRepository<JunglebookPostEntity, Long> {

    @Query("SELECT p FROM JunglebookPostEntity p WHERE p.junglebookId = :junglebookId AND p.camp = :camp " +
            "ORDER BY p.likeCnt DESC, p.viewCnt DESC")
    fun findPopularByCamp(@Param("junglebookId") junglebookId: Int, @Param("camp") camp: Int): List<JunglebookPostEntity>

    @Query("SELECT p FROM JunglebookPostEntity p WHERE p.junglebookId = :junglebookId AND p.camp = :camp " +
            "AND p.useYn = true ORDER BY p.createdDt DESC")
    fun findByJunglebookIdAndCampAndUseYnTrueOrderByCreatedDtDesc(@Param("junglebookId") junglebookId: Int, @Param("camp") camp: Int, pageable: Pageable): List<JunglebookPostEntity>

    @Query("SELECT COUNT(p) FROM JunglebookPostEntity p WHERE p.junglebookId = :junglebookId AND p.camp = :camp AND p.useYn = true")
    fun countByJunglebookIdAndCampAndUseYnTrue(@Param("junglebookId") junglebookId: Int, @Param("camp") camp: Int): Int

    @Query("SELECT p FROM JunglebookPostEntity p WHERE p.id = :id AND p.junglebookId = :junglebookId AND p.useYn = true")
    fun findByIdAndJunglebookIdAndUseYnTrue(@Param("id") id: Long, @Param("junglebookId") junglebookId: Int): JunglebookPostEntity?

    @Modifying
    @Query("UPDATE JunglebookPostEntity p SET p.viewCnt = p.viewCnt + 1 WHERE p.id = :id")
    fun increaseViewCount(@Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE JunglebookPostEntity p SET p.likeCnt = p.likeCnt + 1 WHERE p.id = :id")
    fun increaseLikeCount(@Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE JunglebookPostEntity p SET p.dislikeCnt = p.dislikeCnt + 1 WHERE p.id = :id")
    fun increaseDislikeCount(@Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE JunglebookPostEntity p SET p.useYn = false WHERE p.id = :id AND p.userId = :userId")
    fun softDelete(@Param("id") id: Long, @Param("userId") userId: Long): Int
}