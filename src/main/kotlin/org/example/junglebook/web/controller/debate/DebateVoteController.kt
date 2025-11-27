package org.example.junglebook.web.controller.debate

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.junglebook.enums.VoteType
import org.example.junglebook.model.Member
import org.example.junglebook.service.MemberService
import org.example.junglebook.service.debate.DebateVoteService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "토론 투표")
@Validated
@RestController
@RequestMapping("/api/debate/arguments/{argumentId}/votes")
class DebateVoteController(
    private val debateVoteService: DebateVoteService,
    private val memberService: MemberService
) {

    @Operation(summary = "투표")
    @PostMapping
    fun vote(
        @AuthenticationPrincipal member: Member,
        @PathVariable argumentId: Long,
        @RequestParam voteType: VoteType
    ): ResponseEntity<Map<String, Boolean>> {
        val memberId = getMemberId(member)
        val result = debateVoteService.vote(argumentId, memberId, voteType)
        return ResponseEntity.ok(mapOf("success" to result))
    }

    @Operation(summary = "투표 취소")
    @DeleteMapping
    fun cancelVote(
        @AuthenticationPrincipal member: Member,
        @PathVariable argumentId: Long,
        @RequestParam voteType: VoteType
    ): ResponseEntity<Map<String, Boolean>> {
        val memberId = getMemberId(member)
        val result = debateVoteService.cancelVote(argumentId, memberId, voteType)
        return ResponseEntity.ok(mapOf("success" to result))
    }

    private fun getMemberId(member: Member): Long {
        val memberEntity = memberService.findActivateMemberByLoginId(member.loginId)
        return memberEntity.id ?: throw org.example.junglebook.exception.GlobalException(
            org.example.junglebook.exception.DefaultErrorCode.USER_NOT_FOUND
        )
    }
}

