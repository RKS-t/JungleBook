package org.example.junglebook.entity.report


import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "report")
@EntityListeners(AuditingEntityListener::class)
data class ReportEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    val reportId: Long? = null,

    @Column(name = "ref_type")
    val refType: Int? = null,

    @Column(name = "target_id", nullable = false)
    val targetId: Long,

    @Column(name = "type")
    val type: Int? = null,

    @Column(name = "message", length = 1000)
    val message: String? = null,

    @Column(name = "user_id")
    val userId: Long? = null,

    @CreatedDate
    @Column(name = "created_dt", updatable = false, nullable = false)
    val createdDt: LocalDateTime = LocalDateTime.now()
)