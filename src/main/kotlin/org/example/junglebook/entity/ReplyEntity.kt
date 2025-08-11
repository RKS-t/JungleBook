package org.example.junglebook.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "reply")
data class ReplyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    val id: Long? = null,

    // 이 댓글이 달린 주장 (N:1)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "argument_id", nullable = false)
    val argument: ArgumentEntity,

    // 댓글 작성자 (N:1)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    val author: MemberEntity,

    // 부모 댓글 (대댓글 기능)
    // 최상위 댓글의 경우 부모가 없으므로 nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_reply_id")
    val parent: ReplyEntity? = null,

    // 자식 댓글 목록 (대댓글 기능)
    // mappedBy="parent"는 ReplyEntity의 'parent' 필드에 의해 매핑됨을 의미
    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL])
    val children: MutableList<ReplyEntity> = mutableListOf(),

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    var upvotes: Long = 0,
    var downvotes: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
)