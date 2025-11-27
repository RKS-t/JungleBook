package org.example.junglebook.entity.post

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "post_file", indexes = [
    Index(name = "idx_ref_type_id", columnList = "ref_type, ref_id"),
    Index(name = "idx_user_id", columnList = "user_id"),
    Index(name = "idx_attach_yn", columnList = "attach_yn")
])
@EntityListeners(AuditingEntityListener::class)
data class PostFileEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "attach_yn")
    var attachYn: Boolean = false,

    @Column(name = "ref_type")
    val refType: Int? = null,

    @Column(name = "ref_id")
    val refId: Long? = null,

    @Column(name = "user_id")
    val userId: Long? = null,

    @Column(name = "file_type", length = 50)
    val fileType: String? = null,

    @Column(name = "file_size", length = 20)
    val fileSize: String? = null,

    @Column(name = "file_name", length = 255)
    val fileName: String? = null,

    @Column(name = "url", length = 500)
    val url: String? = null,

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)