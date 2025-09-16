package org.example.junglebook.entity.debate

import jakarta.persistence.*
import org.example.junglebook.enums.DebateTopicCategory
import org.example.junglebook.enums.DebateTopicStatus
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate
import java.time.LocalDateTime
@Entity
@Table(name = "debate_topic")
@EntityListeners(AuditingEntityListener::class)
data class DebateTopicEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 200)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(name = "description_html", columnDefinition = "TEXT")
    val descriptionHtml: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val category: DebateTopicCategory,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: DebateTopicStatus,

    @Column(name = "creator_id")
    val creatorId: Long? = null,

    @Column(name = "hot_yn")
    var hotYn: Boolean = false,

    @Column(name = "active_yn")
    var activeYn: Boolean = true,

    // 토론 기간
    @Column(name = "start_date")
    val startDate: LocalDate? = null,

    @Column(name = "end_date")
    val endDate: LocalDate? = null,

    // 통계
    @Column(name = "argument_count")
    var argumentCount: Int = 0,

    @Column(name = "view_count")
    var viewCount: Int = 0,

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun increaseViewCount() {
        this.viewCount++
        this.updatedAt = LocalDateTime.now()
    }

    fun increaseArgumentCount() {
        this.argumentCount++
        this.updatedAt = LocalDateTime.now()
    }
}