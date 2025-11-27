package org.example.junglebook.entity.post

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "post", indexes = [
    Index(name = "idx_board_created", columnList = "board_id, created_dt"),
    Index(name = "idx_user_created", columnList = "user_id, created_dt"),
    Index(name = "idx_notice_created", columnList = "notice_yn, created_dt"),
    Index(name = "idx_use_yn", columnList = "use_yn")
])
@EntityListeners(AuditingEntityListener::class)
data class PostEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "board_id", nullable = false)
    val boardId: Int,

    @Column(name = "seq_no")
    val seqNo: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "notice_yn")
    var noticeYn: Boolean = false,

    @Column(name = "use_yn")
    var useYn: Boolean = true,

    @Column(name = "file_yn")
    val fileYn: Boolean = false,

    @Column(name = "author_nickname", length = 50)
    val authorNickname: String,

    @Column(name = "title", length = 200, nullable = false)
    var title: String,

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(name = "content_html", columnDefinition = "TEXT")
    var contentHtml: String? = null,

    @Column(name = "view_cnt")
    var viewCnt: Int = 0,

    @Column(name = "like_cnt")
    var likeCnt: Int = 0,

    @Column(name = "dislike_cnt")
    var dislikeCnt: Int = 0,

    @Column(name = "reply_cnt")
    var replyCnt: Int = 0,

    @CreatedDate
    @Column(name = "created_dt", updatable = false, nullable = false)
    val createdDt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "updated_dt", nullable = false)
    var updatedDt: LocalDateTime = LocalDateTime.now()
) {
    // 게시판과의 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", insertable = false, updatable = false)
    val board: BoardEntity? = null

    // 파일 연관관계
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "ref_id")
    val files: List<PostFileEntity> = emptyList()

    // 조회수 증가
    fun increaseViewCount() {
        this.viewCnt++
        this.updatedDt = LocalDateTime.now()
    }

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

    // 댓글 수 업데이트
    fun updateReplyCount(count: Int) {
        this.replyCnt = count
        this.updatedDt = LocalDateTime.now()
    }

    // 게시글 삭제 (소프트 삭제)
    fun softDelete() {
        this.useYn = false
        this.updatedDt = LocalDateTime.now()
    }

    // 공지사항 설정/해제
    fun setNotice() {
        this.noticeYn = true
        this.updatedDt = LocalDateTime.now()
    }

    fun unsetNotice() {
        this.noticeYn = false
        this.updatedDt = LocalDateTime.now()
    }
}