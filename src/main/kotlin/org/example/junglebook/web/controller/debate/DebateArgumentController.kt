package org.example.junglebook.web.controller.debate

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.junglebook.enums.ArgumentStance
import org.example.junglebook.model.Member
import org.example.junglebook.service.MemberService
import org.example.junglebook.service.debate.DebateArgumentService
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
    private val debateArgumentService: DebateArgumentService,
    private val memberService: MemberService
) {

    @Operation(summary = "인기 논증 목록 조회")
    @GetMapping("/popular")
    fun getPopularArguments(
        @PathVariable topicId: Long
    ): ResponseEntity<Map<ArgumentStance, List<DebateArgumentSimpleResponse>>> {
        val popularArguments = debateArgumentService.popularList(topicId)
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
        val argumentList = debateArgumentService.pageableList(topicId, stance, pageNo, limit)
        return ResponseEntity.ok(argumentList)
    }

    @Operation(summary = "논증 상세 조회")
    @GetMapping("/{argumentId}")
    fun getArgumentDetail(
        @PathVariable topicId: Long,
        @PathVariable argumentId: Long
    ): ResponseEntity<DebateArgumentResponse> {
        val argument = debateArgumentService.view(topicId, argumentId)
            ?: return ResponseEntity.notFound().build()
        
        debateArgumentService.increaseViewCount(argumentId)
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
        val entity = request.toEntity(topicId, memberId)
        val argumentResponse = debateArgumentService.createArgument(entity, request.fileIds)
        
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
        val deleted = debateArgumentService.deleteArgument(topicId, argumentId, memberId)
        
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(summary = "작성자별 논증 조회")
    @GetMapping("/author/{userId}")
    fun getArgumentsByAuthor(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<DebateArgumentListResponse> {
        val argumentList = debateArgumentService.getArgumentsByAuthor(userId, pageNo, limit)
        return ResponseEntity.ok(argumentList)
    }

    @Operation(summary = "토픽별 입장 통계 조회")
    @GetMapping("/statistics")
    fun getTopicStatistics(
        @PathVariable topicId: Long
    ): ResponseEntity<Map<ArgumentStance, Int>> {
        val statistics = debateArgumentService.getTopicStatistics(topicId)
        return ResponseEntity.ok(statistics)
    }

    @Operation(summary = "지지 토글")
    @PostMapping("/{argumentId}/support")
    fun toggleSupport(
        @PathVariable argumentId: Long,
        @RequestParam increase: Boolean
    ): ResponseEntity<Map<String, Boolean>> {
        val result = debateArgumentService.toggleSupport(argumentId, increase)
        return ResponseEntity.ok(mapOf("success" to result))
    }

    @Operation(summary = "반대 토글")
    @PostMapping("/{argumentId}/oppose")
    fun toggleOppose(
        @PathVariable argumentId: Long,
        @RequestParam increase: Boolean
    ): ResponseEntity<Map<String, Boolean>> {
        val result = debateArgumentService.toggleOppose(argumentId, increase)
        return ResponseEntity.ok(mapOf("success" to result))
    }

    @Operation(summary = "전체 논증 목록 조회")
    @GetMapping
    fun getAllArguments(
        @PathVariable topicId: Long,
        @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<DebateArgumentListResponse> {
        val argumentList = debateArgumentService.getAllArgumentsByTopic(topicId, pageNo, limit)
        return ResponseEntity.ok(argumentList)
    }

    private fun getMemberId(member: Member): Long {
        val memberEntity = memberService.findActivateMemberByLoginId(member.loginId)
        return memberEntity.id ?: throw org.example.junglebook.exception.GlobalException(
            org.example.junglebook.exception.DefaultErrorCode.USER_NOT_FOUND
        )
    }
}

