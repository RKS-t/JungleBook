package org.example.junglebook.entity

import jakarta.persistence.*
import org.example.junglebook.enums.DebateTopicCategory
import org.example.junglebook.enums.DebateTopicStatus
import java.time.LocalDateTime

@Entity
@Table(name = "debate_topic")
data class DebateTopicEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_id")
    val id: Long? = null,

    //논제 생성자 필요한가?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    val creator: MemberEntity?,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var category: DebateTopicCategory,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: DebateTopicStatus,

    // 조회수
    @Column(name = "view_count")
    var viewCount: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
)