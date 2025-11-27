package org.example.junglebook.entity.debate

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "debate_argument_fallacy_training_data", indexes = [
    Index(name = "idx_used_training", columnList = "used_for_training"),
    Index(name = "idx_label", columnList = "label")
])
@EntityListeners(AuditingEntityListener::class)
data class DebateArgumentFallacyTrainingDataEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "argument_id")
    val argumentId: Long? = null,

    @Column(name = "text", columnDefinition = "TEXT", nullable = false)
    val text: String,

    @Column(name = "label", length = 50, nullable = false)
    val label: String,

    @Column(name = "source", length = 50, nullable = false)
    val source: String,

    @Column(name = "used_for_training", nullable = false)
    var usedForTraining: Boolean = false,

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        const val SOURCE_USER_APPEAL = "user_appeal"
        const val SOURCE_AI_VERIFIED = "ai_verified"
        const val SOURCE_MANUAL = "manual"
    }
}

