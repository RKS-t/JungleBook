package org.example.junglebook.entity

import jakarta.persistence.*
import org.example.junglebook.enums.VoteType
import java.time.LocalDateTime

@Entity
@Table(
    name = "vote",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_vote_member_argument", columnNames = ["member_id", "argument_id"]),
        UniqueConstraint(name = "uk_vote_member_reply", columnNames = ["member_id", "reply_id"])
    ]
)
data class VoteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_id")
    val id: Long? = null,

    // 투표를 한 회원
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    val member: MemberEntity,

    // 투표 대상이 '주장'인 경우. 대상이 '댓글'이면 null.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "argument_id")
    val argument: ArgumentEntity? = null,

    // 투표 대상이 '댓글'인 경우. 대상이 '주장'이면 null.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_id")
    val reply: ReplyEntity? = null,

    @Column(name = "vote_type", nullable = false)
    @Enumerated(EnumType.STRING)
    val voteType: VoteType,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,
)