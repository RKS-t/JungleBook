package org.example.junglebook.entity.debate


import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "junglebook")
@EntityListeners(AuditingEntityListener::class)
data class JunglebookEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "use_yn")
    var useYn: Int = 1,

    @Column(name = "hot_yn")
    var hotYn: Int = 0,

    @Column(name = "post_cnt")
    val postCnt: Int = 0,

    @Column(name = "title", length = 200, nullable = false)
    val title: String,

    @Column(name = "content", columnDefinition = "TEXT")
    val content: String,

    @Column(name = "content_html", columnDefinition = "TEXT")
    val contentHtml: String? = null,

    @Column(name = "start_date")
    val startDate: LocalDate? = null,

    @Column(name = "end_date")
    val endDate: LocalDate? = null,

    @CreatedDate
    @Column(name = "created_dt", updatable = false, nullable = false)
    val createdDt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "updated_dt", nullable = false)
    var updatedDt: LocalDateTime = LocalDateTime.now()
)