package org.example.junglebook.entity

import jakarta.persistence.*
import org.example.junglebook.enums.ArgumentStance
import java.time.LocalDateTime

@Entity
@Table(name = "argument")
data class ArgumentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "argument_id")
    val id: Long? = null,

    // 이 주장이 속한 토론 논제 (N:1)
    // 주장은 반드시 특정 논제에 속해야 하므로 nullable = false
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    val topic: DebateTopicEntity,

    // 이 주장을 작성한 회원 (N:1)
    // 주장은 반드시 작성자가 있어야 하므로 nullable = false
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    val author: MemberEntity,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var stance: ArgumentStance,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    // 추천 수
    var upvotes: Long = 0,

    // 비추천 수
    var downvotes: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
)