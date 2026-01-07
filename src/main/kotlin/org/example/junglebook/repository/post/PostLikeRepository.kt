package org.example.junglebook.repository.post

import org.example.junglebook.entity.post.PostLikeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostLikeRepository : JpaRepository<PostLikeEntity, Long> {

    fun findByMemberIdAndPostId(memberId: Long, postId: Long): PostLikeEntity?

    fun findByMemberIdAndReplyId(memberId: Long, replyId: Long): PostLikeEntity?

    fun existsByMemberIdAndPostId(memberId: Long, postId: Long): Boolean

    fun existsByMemberIdAndReplyId(memberId: Long, replyId: Long): Boolean

    fun countByPostId(postId: Long): Long

    fun countByReplyId(replyId: Long): Long

    fun findByMemberIdAndPostIdIsNotNull(memberId: Long): List<PostLikeEntity>

    fun findByMemberIdAndReplyIdIsNotNull(memberId: Long): List<PostLikeEntity>
}

