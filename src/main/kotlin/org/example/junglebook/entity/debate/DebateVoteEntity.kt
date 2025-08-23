package org.example.junglebook.entity.debate

import jakarta.persistence.*
import org.example.junglebook.enums.VoteType
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(
    name = "debate_vote",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_vote_member_argument", columnNames = ["member_id", "argument_id", "vote_type"]),
        UniqueConstraint(name = "uk_vote_member_reply", columnNames = ["member_id", "reply_id", "vote_type"])
    ]
)
@EntityListeners(AuditingEntityListener::class)
data class DebateVoteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    // 투표 대상 (주장 또는 댓글 중 하나)
    @Column(name = "argument_id")
    val argumentId: Long? = null,

    @Column(name = "reply_id")
    val replyId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false)
    val voteType: VoteType,

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require((argumentId != null && replyId == null) || (argumentId == null && replyId != null)) {
            "Either argumentId or replyId must be set, but not both"
        }
    }
}