package org.example.junglebook.repository.debate

import org.example.junglebook.entity.debate.JunglebookCountHistoryEntity
import org.example.junglebook.enums.debate.JunglebookReferenceType
import org.example.junglebook.enums.post.CountType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface JunglebookCountHistoryRepository : JpaRepository<JunglebookCountHistoryEntity, Long> {

    fun countByRefTypeAndRefIdAndUserId(
        refType: JunglebookReferenceType,
        refId: Long,
        userId: Long
    ): Int

    fun existsByRefTypeAndRefIdAndUserIdAndType(
        refType: JunglebookReferenceType,
        refId: Long,
        userId: Long,
        type: CountType
    ): Boolean

    fun findByRefTypeAndRefIdAndUserId(
        refType: JunglebookReferenceType,
        refId: Long,
        userId: Long
    ): List<JunglebookCountHistoryEntity>

}