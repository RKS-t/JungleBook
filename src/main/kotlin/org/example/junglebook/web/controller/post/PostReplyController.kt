package org.example.junglebook.web.controller.post

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.junglebook.enums.post.CountType
import org.example.junglebook.model.Member
import org.example.junglebook.service.MemberService
import org.example.junglebook.service.post.PostReplyService
import org.example.junglebook.web.dto.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "게시판 댓글")
@Validated
@RestController
@RequestMapping("/api/posts/{postId}/replies")
class PostReplyController(
    private val postReplyService: PostReplyService,
    private val memberService: MemberService
) {

    @Operation(summary = "댓글 목록 조회")
    @GetMapping
    fun getReplyList(
        @PathVariable postId: Long
    ): ResponseEntity<List<PostReplyResponse>> {
        val replies = postReplyService.postReplyList(postId)
        val responses = replies.map { PostReplyResponse.of(it) }
        return ResponseEntity.ok(responses)
    }

    @Operation(summary = "댓글 생성")
    @PostMapping
    fun createReply(
        @AuthenticationPrincipal member: Member,
        @PathVariable postId: Long,
        @RequestBody request: PostReplyCreateRequest
    ): ResponseEntity<PostReplyResponse> {
        val memberEntity = getMemberEntity(member)
        val authorNickname = memberEntity.nickname ?: "익명"
        val savedEntity = postReplyService.create(postId, request, memberEntity.id!!, authorNickname)
        return ResponseEntity.status(HttpStatus.CREATED).body(PostReplyResponse.of(savedEntity))
    }

    @Operation(summary = "댓글 수정")
    @PutMapping("/{replyId}")
    fun updateReply(
        @AuthenticationPrincipal member: Member,
        @PathVariable postId: Long,
        @PathVariable replyId: Long,
        @RequestBody request: PostReplyUpdateRequest
    ): ResponseEntity<PostReplyResponse> {
        val memberEntity = getMemberEntity(member)
        val authorNickname = memberEntity.nickname ?: "익명"
        val updatedEntity = postReplyService.modify(postId, replyId, request, memberEntity.id!!, authorNickname)
        return ResponseEntity.ok(PostReplyResponse.of(updatedEntity))
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{replyId}")
    fun deleteReply(
        @AuthenticationPrincipal member: Member,
        @PathVariable postId: Long,
        @PathVariable replyId: Long
    ): ResponseEntity<Void> {
        val memberId = getMemberId(member)
        postReplyService.remove(postId, replyId, memberId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "댓글 좋아요 증가")
    @PostMapping("/{replyId}/like")
    fun increaseLike(
        @AuthenticationPrincipal member: Member,
        @PathVariable postId: Long,
        @PathVariable replyId: Long
    ): ResponseEntity<CountResponse> {
        val memberId = getMemberId(member)
        val count = postReplyService.increaseCount(postId, replyId, memberId, CountType.LIKE)
        return ResponseEntity.ok(CountResponse(count))
    }

    private fun getMemberId(member: Member): Long {
        val memberEntity = getMemberEntity(member)
        return requireNotNull(memberEntity.id) {
            "Member ID must not be null"
        }
    }

    private fun getMemberEntity(member: Member): org.example.junglebook.entity.MemberEntity {
        return memberService.findActivateMemberByLoginId(member.loginId)
    }
}

