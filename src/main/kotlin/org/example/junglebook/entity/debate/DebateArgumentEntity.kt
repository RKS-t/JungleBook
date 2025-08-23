package org.example.junglebook.entity.debate

import jakarta.persistence.*
import org.example.junglebook.enums.ArgumentStance
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime


@Entity
@Table(name = "debate_argument", indexes = [
    Index(name = "idx_topic_created", columnList = "topic_id, created_at"),
    Index(name = "idx_stance_created", columnList = "stance, created_at")
])
@EntityListeners(AuditingEntityListener::class)
data class DebateArgumentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "topic_id", nullable = false)
    val topicId: Long,

    @Column(name = "author_id", nullable = false)
    val authorId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "stance", nullable = false)
    val stance: ArgumentStance,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "content_html", columnDefinition = "TEXT")
    var contentHtml: String? = null,

    // 작성자 닉네임 (성능을 위한 비정규화)
    @Column(name = "author_nickname", length = 50)
    val authorNickname: String,

    // 상태 관리
    @Column(name = "active_yn")
    var activeYn: Boolean = true,

    @Column(name = "notice_yn")
    var noticeYn: Boolean = false,

    @Column(name = "file_yn")
    val fileYn: Boolean = false,

    // 통계
    @Column(name = "view_count")
    var viewCount: Int = 0,

    @Column(name = "support_count")
    var supportCount: Int = 0,

    @Column(name = "oppose_count")
    var opposeCount: Int = 0,

    @Column(name = "reply_count")
    var replyCount: Int = 0,

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

    fun toggleSupport(increase: Boolean) {
        if (increase) this.supportCount++ else this.supportCount = maxOf(0, this.supportCount - 1)
        this.updatedAt = LocalDateTime.now()
    }

    fun toggleOppose(increase: Boolean) {
        if (increase) this.opposeCount++ else this.opposeCount = maxOf(0, this.opposeCount - 1)
        this.updatedAt = LocalDateTime.now()
    }

    fun softDelete() {
        this.activeYn = false
        this.updatedAt = LocalDateTime.now()
    }
}
