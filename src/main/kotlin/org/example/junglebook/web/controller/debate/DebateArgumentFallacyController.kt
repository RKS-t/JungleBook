package org.example.junglebook.web.controller.debate

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.junglebook.model.Member
import org.example.junglebook.service.MemberService
import org.example.junglebook.service.fallacy.FallacyAppealService
import org.example.junglebook.service.fallacy.FallacyRetrainingService
import org.example.junglebook.web.dto.FallacyAppealRequest
import org.example.junglebook.web.dto.FallacyAppealResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "토론 논증 논리 오류")
@Validated
@RestController
@RequestMapping("/api/debate/arguments/{argumentId}/fallacy")
class DebateArgumentFallacyController(
    private val fallacyAppealService: FallacyAppealService,
    private val fallacyRetrainingService: FallacyRetrainingService,
    private val memberService: MemberService
) {

    @Operation(summary = "의의 제기")
    @PostMapping("/appeal")
    fun createAppeal(
        @AuthenticationPrincipal member: Member,
        @PathVariable argumentId: Long,
        @RequestBody request: FallacyAppealRequest
    ): ResponseEntity<FallacyAppealResponse> {
        val memberId = getMemberId(member)
        val appeal = fallacyAppealService.createAppeal(argumentId, memberId, request.appealReason)
        
        val response = FallacyAppealResponse(
            id = appeal.id ?: 0L,
            argumentId = appeal.argumentId,
            appealerId = appeal.appealerId,
            appealReason = appeal.appealReason,
            status = appeal.status,
            createdAt = appeal.createdAt.toString()
        )
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "논증의 의의 목록 조회")
    @GetMapping("/appeals")
    fun getAppeals(
        @PathVariable argumentId: Long
    ): ResponseEntity<List<FallacyAppealResponse>> {
        val appeals = fallacyAppealService.getAppealsByArgument(argumentId)
        val responses = appeals.map { appeal ->
            FallacyAppealResponse(
                id = appeal.id ?: 0L,
                argumentId = appeal.argumentId,
                appealerId = appeal.appealerId,
                appealReason = appeal.appealReason,
                status = appeal.status,
                createdAt = appeal.createdAt.toString()
            )
        }
        return ResponseEntity.ok(responses)
    }

    @Operation(summary = "의의 개수 조회")
    @GetMapping("/appeals/count")
    fun getAppealCount(
        @PathVariable argumentId: Long
    ): ResponseEntity<Map<String, Int>> {
        val totalCount = fallacyAppealService.countAppealsByArgument(argumentId)
        val pendingCount = fallacyAppealService.countPendingAppealsByArgument(argumentId)
        
        return ResponseEntity.ok(mapOf(
            "total" to totalCount,
            "pending" to pendingCount
        ))
    }

    @Operation(summary = "대기 중인 의의 목록 조회")
    @GetMapping("/appeals/pending")
    fun getPendingAppeals(): ResponseEntity<List<FallacyAppealResponse>> {
        val appeals = fallacyAppealService.getPendingAppeals()
        val responses = appeals.map { appeal ->
            FallacyAppealResponse(
                id = appeal.id ?: 0L,
                argumentId = appeal.argumentId,
                appealerId = appeal.appealerId,
                appealReason = appeal.appealReason,
                status = appeal.status,
                createdAt = appeal.createdAt.toString()
            )
        }
        return ResponseEntity.ok(responses)
    }

    @Operation(summary = "의의 승인 (관리자)")
    @PostMapping("/appeals/{appealId}/approve")
    fun approveAppeal(
        @AuthenticationPrincipal member: Member,
        @PathVariable appealId: Long
    ): ResponseEntity<FallacyAppealResponse> {
        val memberId = getMemberId(member)
        val appeal = fallacyAppealService.approveAppeal(appealId, memberId)
        
        val response = FallacyAppealResponse(
            id = appeal.id ?: 0L,
            argumentId = appeal.argumentId,
            appealerId = appeal.appealerId,
            appealReason = appeal.appealReason,
            status = appeal.status,
            createdAt = appeal.createdAt.toString()
        )
        
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "의의 거부 (관리자)")
    @PostMapping("/appeals/{appealId}/reject")
    fun rejectAppeal(
        @AuthenticationPrincipal member: Member,
        @PathVariable appealId: Long
    ): ResponseEntity<FallacyAppealResponse> {
        val memberId = getMemberId(member)
        val appeal = fallacyAppealService.rejectAppeal(appealId, memberId)
        
        val response = FallacyAppealResponse(
            id = appeal.id ?: 0L,
            argumentId = appeal.argumentId,
            appealerId = appeal.appealerId,
            appealReason = appeal.appealReason,
            status = appeal.status,
            createdAt = appeal.createdAt.toString()
        )
        
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "재학습 트리거 (관리자)")
    @PostMapping("/retrain")
    fun triggerRetraining(
        @AuthenticationPrincipal member: Member
    ): ResponseEntity<Map<String, String>> {
        fallacyRetrainingService.triggerRetraining()
        return ResponseEntity.ok(mapOf("status" to "retraining_triggered"))
    }

    @Operation(summary = "재학습 데이터 수집 (관리자)")
    @PostMapping("/collect-training-data")
    fun collectTrainingData(
        @AuthenticationPrincipal member: Member
    ): ResponseEntity<Map<String, String>> {
        fallacyRetrainingService.collectTrainingData()
        val count = fallacyRetrainingService.countUnusedTrainingData()
        return ResponseEntity.ok(mapOf(
            "status" to "data_collected",
            "unused_count" to count.toString()
        ))
    }

    @Operation(summary = "미사용 재학습 데이터 개수 조회")
    @GetMapping("/training-data/count")
    fun getTrainingDataCount(): ResponseEntity<Map<String, Long>> {
        val count = fallacyRetrainingService.countUnusedTrainingData()
        return ResponseEntity.ok(mapOf("unused_count" to count))
    }

    private fun getMemberId(member: Member): Long {
        return memberService.getMemberId(member)
    }
}
