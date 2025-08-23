package org.example.junglebook.entity.debate


import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "junglebook_post_reply")
@EntityListeners(AuditingEntityListener::class)
data class JunglebookPostReplyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "junglebook_id")
    val junglebookId: Int? = null,

    @Column(name = "post_id", nullable = false)
    val postId: Long,

    @Column(name = "pid")
    val pid: Long? = null,

    @Column(name = "user_id")
    val userId: Long? = null,

    @Column(name = "use_yn")
    var useYn: Boolean = true,

    @Column(name = "file_yn")
    val fileYn: Boolean = false,

    @Column(name = "nickname", length = 50)
    val nickname: String? = null,

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    val content: String,

    @Column(name = "content_html", columnDefinition = "TEXT")
    val contentHtml: String? = null,

    @Column(name = "like_cnt")
    var likeCnt: Int = 0,

    @Column(name = "dislike_cnt")
    var dislikeCnt: Int = 0,

    @CreatedDate
    @Column(name = "created_dt", updatable = false, nullable = false)
    val createdDt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "updated_dt", nullable = false)
    var updatedDt: LocalDateTime = LocalDateTime.now()
) {
    // 게시글과의 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    val post: JunglebookPostEntity? = null

    // 부모 댓글과의 연관관계 (대댓글인 경우)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pid", insertable = false, updatable = false)
    val parentReply: JunglebookPostReplyEntity? = null

    // 좋아요 증가/감소
    fun increaseLike() {
        this.likeCnt++
        this.updatedDt = LocalDateTime.now()
    }

    fun decreaseLike() {
        if (this.likeCnt > 0) {
            this.likeCnt--
            this.updatedDt = LocalDateTime.now()
        }
    }

    // 싫어요 증가/감소
    fun increaseDislike() {
        this.dislikeCnt++
        this.updatedDt = LocalDateTime.now()
    }

    fun decreaseDislike() {
        if (this.dislikeCnt > 0) {
            this.dislikeCnt--
            this.updatedDt = LocalDateTime.now()
        }
    }

    // 댓글 삭제 (소프트 삭제)
    fun softDelete() {
        this.useYn = false
        this.updatedDt = LocalDateTime.now()
    }

    // 대댓글 여부 확인
    fun isChildReply(): Boolean = pid != null
}