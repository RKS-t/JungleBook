package org.example.junglebook.entity.post

import jakarta.persistence.*
import org.example.junglebook.enums.post.CountType
import org.example.junglebook.enums.post.PostReferenceType
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "post_count_history")
@EntityListeners(AuditingEntityListener::class)
data class PostCountHistoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false)
    val refType: PostReferenceType,

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
