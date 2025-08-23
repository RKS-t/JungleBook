package org.example.junglebook.entity.post


import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "board")
@EntityListeners(AuditingEntityListener::class)
data class BoardEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(name = "use_yn")
    var useYn: Boolean = true,

    @Column(name = "type")
    val type: Int? = null,

    @Column(name = "name", length = 100, nullable = false)
    val name: String,

    @CreatedDate
    @Column(name = "created_dt", updatable = false, nullable = false)
    val createdDt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "updated_dt", nullable = false)
    var updatedDt: LocalDateTime = LocalDateTime.now()
)