package org.example.junglebook.repository.post

import org.example.junglebook.entity.post.PostLikeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostLikeRepository : JpaRepository<PostLikeEntity, Long> {

    // 게시글 좋아요 조회
    fun findByMemberIdAndPostId(memberId: Long, postId: Long): PostLikeEntity?

    // 댓글 좋아요 조회
    fun findByMemberIdAndReplyId(memberId: Long, replyId: Long): PostLikeEntity?

    // 게시글 좋아요 존재 여부
    fun existsByMemberIdAndPostId(memberId: Long, postId: Long): Boolean

    // 댓글 좋아요 존재 여부
    fun existsByMemberIdAndReplyId(memberId: Long, replyId: Long): Boolean

    // 게시글의 좋아요 개수
    fun countByPostId(postId: Long): Long

    // 댓글의 좋아요 개수
    fun countByReplyId(replyId: Long): Long

    // 특정 회원이 좋아요한 게시글 목록
    fun findByMemberIdAndPostIdIsNotNull(memberId: Long): List<PostLikeEntity>

    // 특정 회원이 좋아요한 댓글 목록
    fun findByMemberIdAndReplyIdIsNotNull(memberId: Long): List<PostLikeEntity>
}

