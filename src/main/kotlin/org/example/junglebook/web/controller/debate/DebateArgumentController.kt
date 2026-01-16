package org.example.junglebook.web.controller.debate

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.junglebook.enums.ArgumentStance
import org.example.junglebook.model.Member
import org.example.junglebook.service.MemberService
import org.example.junglebook.service.debate.DebateArgumentCommandService
import org.example.junglebook.service.debate.DebateArgumentQueryService
import org.example.junglebook.web.dto.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "토론 논증")
@Validated
@RestController
@RequestMapping("/api/debate/topics/{topicId}/arguments")
class DebateArgumentController(
    private val debateArgumentCommandService: DebateArgumentCommandService,
    private val debateArgumentQueryService: DebateArgumentQueryService,
    private val memberService: MemberService
) {

    @Operation(summary = "인기 논증 목록 조회")
    @GetMapping("/popular")
    fun getPopularArguments(
        @PathVariable topicId: Long
    ): ResponseEntity<Map<ArgumentStance, List<DebateArgumentSimpleResponse>>> {
        val popularArguments = debateArgumentQueryService.getPopularList(topicId)
        return ResponseEntity.ok(popularArguments)
    }

    @Operation(summary = "입장별 논증 목록 조회")
    @GetMapping("/stance/{stance}")
    fun getArgumentsByStance(
        @PathVariable topicId: Long,
        @PathVariable stance: ArgumentStance,
        @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<DebateArgumentListResponse> {
        val argumentList = debateArgumentQueryService.getPageableList(topicId, stance, pageNo, limit)
        return ResponseEntity.ok(argumentList)
    }

    @Operation(summary = "논증 상세 조회")
    @GetMapping("/{argumentId}")
    fun getArgumentDetail(
        @PathVariable topicId: Long,
        @PathVariable argumentId: Long,
        @RequestParam(defaultValue = "true") increaseView: Boolean
    ): ResponseEntity<DebateArgumentResponse> {
        val argument = debateArgumentQueryService.getArgument(topicId, argumentId)
            ?: return ResponseEntity.notFound().build()
        if (increaseView) {
            debateArgumentCommandService.increaseViewCount(argumentId)
        }
        return ResponseEntity.ok(argument)
    }

    @Operation(summary = "논증 생성")
    @PostMapping
    fun createArgument(
        @AuthenticationPrincipal member: Member,
        @PathVariable topicId: Long,
        @RequestBody request: DebateArgumentCreateRequest
    ): ResponseEntity<DebateArgumentResponse> {
        val memberId = getMemberId(member)
        val argumentResponse = debateArgumentCommandService.createArgument(topicId, memberId, request)
        
        return ResponseEntity.status(HttpStatus.CREATED).body(argumentResponse)
    }

    @Operation(summary = "논증 삭제")
    @DeleteMapping("/{argumentId}")
    fun deleteArgument(
        @AuthenticationPrincipal member: Member,
        @PathVariable topicId: Long,
        @PathVariable argumentId: Long
    ): ResponseEntity<Void> {
        val memberId = getMemberId(member)
        debateArgumentCommandService.deleteArgument(topicId, argumentId, memberId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "작성자별 논증 조회")
    @GetMapping("/author/{userId}")
    fun getArgumentsByAuthor(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<DebateArgumentListResponse> {
        val argumentList = debateArgumentQueryService.getArgumentsByAuthor(userId, pageNo, limit)
        return ResponseEntity.ok(argumentList)
    }

    @Operation(summary = "토픽별 입장 통계 조회")
    @GetMapping("/statistics")
    fun getTopicStatistics(
        @PathVariable topicId: Long
    ): ResponseEntity<Map<ArgumentStance, Int>> {
        val statistics = debateArgumentQueryService.getTopicStatistics(topicId)
        return ResponseEntity.ok(statistics)
    }

    @Operation(summary = "지지 토글")
    @PostMapping("/{argumentId}/support")
    fun toggleSupport(
        @PathVariable argumentId: Long,
        @RequestParam increase: Boolean
    ): ResponseEntity<Map<String, Boolean>> {
        val result = debateArgumentCommandService.toggleSupport(argumentId, increase)
        return ResponseEntity.ok(mapOf("success" to result))
    }

    @Operation(summary = "반대 토글")
    @PostMapping("/{argumentId}/oppose")
    fun toggleOppose(
        @PathVariable argumentId: Long,
        @RequestParam increase: Boolean
    ): ResponseEntity<Map<String, Boolean>> {
        val result = debateArgumentCommandService.toggleOppose(argumentId, increase)
        return ResponseEntity.ok(mapOf("success" to result))
    }

    @Operation(summary = "전체 논증 목록 조회")
    @GetMapping
    fun getAllArguments(
        @PathVariable topicId: Long,
        @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<DebateArgumentListResponse> {
        val argumentList = debateArgumentQueryService.getAllArgumentsByTopic(topicId, pageNo, limit)
        return ResponseEntity.ok(argumentList)
    }

    private fun getMemberId(member: Member): Long {
        return memberService.getMemberId(member)
    }
}

