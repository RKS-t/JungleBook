package org.example.junglebook.web.controller.debate

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.junglebook.enums.DebateTopicCategory
import org.example.junglebook.enums.DebateTopicStatus
import org.example.junglebook.model.Member
import org.example.junglebook.service.MemberService
import org.example.junglebook.service.debate.DebateTopicService
import org.example.junglebook.web.dto.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "토론 토픽")
@Validated
@RestController
@RequestMapping("/api/debate/topics")
class DebateTopicController(
    private val debateTopicService: DebateTopicService,
    private val memberService: MemberService
) {

    @Operation(summary = "토픽 생성")
    @PostMapping
    fun createTopic(
        @AuthenticationPrincipal member: Member,
        @RequestBody request: DebateTopicCreateRequest
    ): ResponseEntity<DebateTopicResponse> {
        val memberId = getMemberId(member)
        val topicResponse = debateTopicService.createTopic(request, memberId)
        return ResponseEntity.status(HttpStatus.CREATED).body(topicResponse)
    }

    @Operation(summary = "토픽 상세 조회")
    @GetMapping("/{topicId}")
    fun getTopicDetail(
        @PathVariable topicId: Long,
        @RequestParam(defaultValue = "true") increaseView: Boolean
    ): ResponseEntity<DebateTopicDetailResponse> {
        val topicDetail = debateTopicService.getTopicDetail(topicId, increaseView)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(topicDetail)
    }

    @Operation(summary = "토픽 목록 조회")
    @GetMapping
    fun getTopicList(
        @RequestParam(defaultValue = "LATEST") sortType: TopicSortType,
        @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<DebateTopicListResponse> {
        val topicList = debateTopicService.getTopicList(sortType, pageNo, limit)
        return ResponseEntity.ok(topicList)
    }

    @Operation(summary = "Hot 토픽 조회")
    @GetMapping("/hot")
    fun getHotTopics(
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<DebateTopicSimpleResponse>> {
        val hotTopics = debateTopicService.getHotTopics(limit)
        return ResponseEntity.ok(hotTopics)
    }

    @Operation(summary = "카테고리별 토픽 조회")
    @GetMapping("/category/{category}")
    fun getTopicsByCategory(
        @PathVariable category: DebateTopicCategory,
        @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<DebateTopicListResponse> {
        val topicList = debateTopicService.getTopicsByCategory(category, pageNo, limit)
        return ResponseEntity.ok(topicList)
    }

    @Operation(summary = "토픽 검색")
    @PostMapping("/search")
    fun searchTopics(
        @RequestBody request: DebateTopicSearchRequest
    ): ResponseEntity<DebateTopicListResponse> {
        val topicList = debateTopicService.searchTopics(request)
        return ResponseEntity.ok(topicList)
    }

    @Operation(summary = "진행 중인 토픽 조회")
    @GetMapping("/ongoing")
    fun getOngoingTopics(
        @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<DebateTopicListResponse> {
        val topicList = debateTopicService.getOngoingTopics(pageNo, limit)
        return ResponseEntity.ok(topicList)
    }

    @Operation(summary = "마감 임박 토픽 조회")
    @GetMapping("/ending-soon")
    fun getEndingSoonTopics(
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<DebateTopicSimpleResponse>> {
        val topics = debateTopicService.getEndingSoonTopics(limit)
        return ResponseEntity.ok(topics)
    }

    @Operation(summary = "토픽 수정")
    @PutMapping("/{topicId}")
    fun updateTopic(
        @AuthenticationPrincipal member: Member,
        @PathVariable topicId: Long,
        @RequestBody request: DebateTopicUpdateRequest
    ): ResponseEntity<DebateTopicResponse> {
        val memberId = getMemberId(member)
        val topicResponse = debateTopicService.updateTopic(topicId, request, memberId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(topicResponse)
    }

    @Operation(summary = "토픽 삭제")
    @DeleteMapping("/{topicId}")
    fun deleteTopic(
        @AuthenticationPrincipal member: Member,
        @PathVariable topicId: Long
    ): ResponseEntity<Void> {
        val memberId = getMemberId(member)
        debateTopicService.deleteTopic(topicId, memberId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "토픽 상태 변경")
    @PatchMapping("/{topicId}/status")
    fun changeTopicStatus(
        @AuthenticationPrincipal member: Member,
        @PathVariable topicId: Long,
        @RequestParam status: DebateTopicStatus
    ): ResponseEntity<DebateTopicResponse> {
        val memberId = getMemberId(member)
        val topicResponse = debateTopicService.changeTopicStatus(topicId, status, memberId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(topicResponse)
    }

    @Operation(summary = "토픽 통계 조회")
    @GetMapping("/{topicId}/statistics")
    fun getTopicStatistics(
        @PathVariable topicId: Long
    ): ResponseEntity<TopicStatistics> {
        val statistics = debateTopicService.getTopicStatistics(topicId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(statistics)
    }

    @Operation(summary = "대시보드 데이터 조회")
    @GetMapping("/dashboard")
    fun getDashboard(): ResponseEntity<DebateTopicDashboardResponse> {
        val dashboard = debateTopicService.getDashboard()
        return ResponseEntity.ok(dashboard)
    }

    private fun getMemberId(member: Member): Long {
        val memberEntity = memberService.findActivateMemberByLoginId(member.loginId)
        return requireNotNull(memberEntity.id) {
            "Member ID must not be null"
        }
    }
}

