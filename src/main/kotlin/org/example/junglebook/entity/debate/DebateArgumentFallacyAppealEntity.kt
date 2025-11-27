package org.example.junglebook.entity.debate

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "debate_argument_fallacy_appeal", indexes = [
    Index(name = "idx_argument_created", columnList = "argument_id, created_at"),
    Index(name = "idx_status", columnList = "status")
])
@EntityListeners(AuditingEntityListener::class)
data class DebateArgumentFallacyAppealEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "argument_id", nullable = false)
    val argumentId: Long,

    @Column(name = "appealer_id", nullable = false)
    val appealerId: Long,

    @Column(name = "appeal_reason", columnDefinition = "TEXT", nullable = false)
    val appealReason: String,

    @Column(name = "ai_original_judgment", columnDefinition = "TEXT")
    val aiOriginalJudgment: String? = null,

    @Column(name = "status", length = 20, nullable = false)
    var status: String = "PENDING",

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_APPROVED = "APPROVED"
        const val STATUS_REJECTED = "REJECTED"
    }

    fun approve() {
        this.status = STATUS_APPROVED
    }

    fun reject() {
        this.status = STATUS_REJECTED
    }

    fun isPending(): Boolean = status == STATUS_PENDING
    fun isApproved(): Boolean = status == STATUS_APPROVED
    fun isRejected(): Boolean = status == STATUS_REJECTED
}

