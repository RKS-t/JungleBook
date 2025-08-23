package org.example.junglebook.entity.debate

import jakarta.persistence.*
import org.example.junglebook.enums.DebateReferenceType
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "debate_file")
@EntityListeners(AuditingEntityListener::class)
data class DebateFileEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    val referenceType: DebateReferenceType,

    @Column(name = "reference_id", nullable = false)
    val referenceId: Long,

    @Column(name = "uploader_id", nullable = false)
    val uploaderId: Long,

    @Column(name = "original_name", length = 255)
    val originalName: String? = null,

    @Column(name = "file_size")
    val fileSize: Long = 0,

    @Column(name = "file_type", length = 100)
    val fileType: String? = null,

    @Column(name = "file_url", length = 500)
    val fileUrl: String,

    @Column(name = "active_yn")
    var activeYn: Boolean = true,

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
