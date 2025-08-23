package org.example.junglebook.entity.debate

import jakarta.persistence.*
import org.example.junglebook.enums.debate.JunglebookReferenceType
import org.example.junglebook.enums.post.CountType
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "junglebook_count_history")
@EntityListeners(AuditingEntityListener::class)
data class JunglebookCountHistoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false)
    val refType: JunglebookReferenceType,

    @Column(name = "ref_id", nullable = false)
    val refId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: CountType,

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)