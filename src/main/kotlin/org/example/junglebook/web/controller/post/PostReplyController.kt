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
        return ResponseEntity.ok(replies)
    }

    @Operation(summary = "댓글 생성")
    @PostMapping
    fun createReply(
        @AuthenticationPrincipal member: Member,
        @PathVariable postId: Long,
        @RequestBody request: PostReplyCreateRequest
    ): ResponseEntity<PostReplyResponse> {
        val memberId = getMemberId(member)
        val replyResponse = postReplyService.create(postId, request, memberId, member.loginId)
        return ResponseEntity.status(HttpStatus.CREATED).body(replyResponse)
    }

    @Operation(summary = "댓글 수정")
    @PutMapping("/{replyId}")
    fun updateReply(
        @AuthenticationPrincipal member: Member,
        @PathVariable postId: Long,
        @PathVariable replyId: Long,
        @RequestBody request: PostReplyUpdateRequest
    ): ResponseEntity<PostReplyResponse> {
        val memberId = getMemberId(member)
        val replyResponse = postReplyService.modify(postId, replyId, request, memberId, member.loginId)
        return ResponseEntity.ok(replyResponse)
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
        return memberService.getMemberId(member)
    }
}

