package org.example.junglebook.web.controller.debate

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.junglebook.model.Member
import org.example.junglebook.service.MemberService
import org.example.junglebook.service.debate.DebateReplyService
import org.example.junglebook.web.dto.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "토론 댓글")
@Validated
@RestController
@RequestMapping("/api/debate/arguments/{argumentId}/replies")
class DebateReplyController(
    private val debateReplyService: DebateReplyService,
    private val memberService: MemberService
) {

    @Operation(summary = "댓글 생성")
    @PostMapping
    fun createReply(
        @AuthenticationPrincipal member: Member,
        @PathVariable argumentId: Long,
        @RequestBody request: DebateReplyCreateRequest
    ): ResponseEntity<DebateReplyResponse> {
        val memberId = getMemberId(member)
        val entity = request.toEntity(argumentId, memberId)
        val replyResponse = debateReplyService.createReply(entity, request.fileIds)
        
        return ResponseEntity.status(HttpStatus.CREATED).body(replyResponse)
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{replyId}")
    fun deleteReply(
        @AuthenticationPrincipal member: Member,
        @PathVariable replyId: Long,
        @RequestParam(defaultValue = "false") deleteChildren: Boolean
    ): ResponseEntity<Void> {
        val memberId = getMemberId(member)
        debateReplyService.deleteReply(replyId, memberId, deleteChildren)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "댓글 상세 조회")
    @GetMapping("/{replyId}")
    fun getReply(
        @PathVariable replyId: Long
    ): ResponseEntity<DebateReplyResponse> {
        val reply = debateReplyService.getReply(replyId)
        return ResponseEntity.ok(reply)
    }

    @Operation(summary = "논증의 댓글 목록 조회")
    @GetMapping
    fun getRepliesByArgument(
        @PathVariable argumentId: Long,
        @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<DebateReplyListResponse> {
        val replyList = debateReplyService.getRepliesByArgument(argumentId, pageNo, limit)
        return ResponseEntity.ok(replyList)
    }

    @Operation(summary = "최상위 댓글 목록 조회")
    @GetMapping("/top-level")
    fun getTopLevelReplies(
        @PathVariable argumentId: Long,
        @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<DebateReplyListResponse> {
        val replyList = debateReplyService.getTopLevelReplies(argumentId, pageNo, limit)
        return ResponseEntity.ok(replyList)
    }

    @Operation(summary = "대댓글 목록 조회")
    @GetMapping("/{replyId}/children")
    fun getChildReplies(
        @PathVariable replyId: Long
    ): ResponseEntity<List<DebateReplySimpleResponse>> {
        val childReplies = debateReplyService.getChildReplies(replyId)
        return ResponseEntity.ok(childReplies)
    }

    @Operation(summary = "작성자별 댓글 조회")
    @GetMapping("/author/{userId}")
    fun getRepliesByAuthor(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<DebateReplyListResponse> {
        val replyList = debateReplyService.getRepliesByAuthor(userId, pageNo, limit)
        return ResponseEntity.ok(replyList)
    }

    @Operation(summary = "인기 댓글 조회")
    @GetMapping("/popular")
    fun getPopularReplies(
        @PathVariable argumentId: Long,
        @RequestParam(defaultValue = "5") limit: Int
    ): ResponseEntity<List<DebateReplySimpleResponse>> {
        val popularReplies = debateReplyService.getPopularReplies(argumentId, limit)
        return ResponseEntity.ok(popularReplies)
    }

    @Operation(summary = "지지 토글")
    @PostMapping("/{replyId}/support")
    fun toggleSupport(
        @PathVariable replyId: Long,
        @RequestParam increase: Boolean
    ): ResponseEntity<Map<String, Boolean>> {
        val result = debateReplyService.toggleSupport(replyId, increase)
        return ResponseEntity.ok(mapOf("success" to result))
    }

    @Operation(summary = "반대 토글")
    @PostMapping("/{replyId}/oppose")
    fun toggleOppose(
        @PathVariable replyId: Long,
        @RequestParam increase: Boolean
    ): ResponseEntity<Map<String, Boolean>> {
        val result = debateReplyService.toggleOppose(replyId, increase)
        return ResponseEntity.ok(mapOf("success" to result))
    }

    @Operation(summary = "댓글 통계 조회")
    @GetMapping("/statistics")
    fun getReplyStatistics(
        @PathVariable argumentId: Long
    ): ResponseEntity<ReplyStatistics> {
        val statistics = debateReplyService.getReplyStatistics(argumentId)
        return ResponseEntity.ok(statistics)
    }

    private fun getMemberId(member: Member): Long {
        val memberEntity = memberService.findActivateMemberByLoginId(member.loginId)
        return requireNotNull(memberEntity.id) {
            "Member ID must not be null"
        }
    }
}

