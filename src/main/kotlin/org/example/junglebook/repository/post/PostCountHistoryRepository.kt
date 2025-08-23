package org.example.junglebook.repository.post

import org.example.junglebook.entity.post.PostCountHistoryEntity
import org.example.junglebook.enums.post.CountType
import org.example.junglebook.enums.post.PostReferenceType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostCountHistoryRepository : JpaRepository<PostCountHistoryEntity, Long> {

    fun countByRefTypeAndRefIdAndUserId(
        refType: PostReferenceType,
        refId: Long,
        userId: Long
    ): Int

    fun existsByRefTypeAndRefIdAndUserIdAndType(
        refType: PostReferenceType,
        refId: Long,
        userId: Long,
        type: CountType
    ): Boolean

    fun findByRefTypeAndRefIdAndUserId(
        refType: PostReferenceType,
        refId: Long,
        userId: Long
    ): List<PostCountHistoryEntity>
}