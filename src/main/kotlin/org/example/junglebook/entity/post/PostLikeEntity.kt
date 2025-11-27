package org.example.junglebook.entity.post

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(
    name = "post_like",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_like_member_post", columnNames = ["member_id", "post_id"]),
        UniqueConstraint(name = "uk_like_member_reply", columnNames = ["member_id", "reply_id"])
    ],
    indexes = [
        Index(name = "idx_member_id", columnList = "member_id"),
        Index(name = "idx_post_id", columnList = "post_id"),
        Index(name = "idx_reply_id", columnList = "reply_id")
    ]
)
@EntityListeners(AuditingEntityListener::class)
data class PostLikeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    // 좋아요 대상 (게시글 또는 댓글 중 하나)
    @Column(name = "post_id")
    val postId: Long? = null,

    @Column(name = "reply_id")
    val replyId: Long? = null,

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require((postId != null && replyId == null) || (postId == null && replyId != null)) {
            "Either postId or replyId must be set, but not both"
        }
    }
}

