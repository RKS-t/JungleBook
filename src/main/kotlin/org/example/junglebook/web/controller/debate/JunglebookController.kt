package org.example.junglebook.web.controller.debate

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.constraints.Min
import org.example.junglebook.service.debate.JunglebookService
import org.example.junglebook.web.dto.JunglebookListResponse
import org.example.junglebook.web.dto.JunglebookResponse

@Tag(name = "정글북 토론")
@Validated
@RestController
@RequestMapping("/api")
class JunglebookController(
    private val junglebookService: JunglebookService
) {

    @Operation(summary = "HOT 정글북")
    @GetMapping("/debate-hot")
    fun hot(): ResponseEntity<JunglebookResponse?> {
        val hotJunglebook = junglebookService.hot()
        return ResponseEntity.ok(hotJunglebook?.let { JunglebookResponse.of(it) })
    }

    @Operation(summary = "진행중인 정글북 목록")
    @GetMapping("/debate-in-progress")
    fun inProgressList(): ResponseEntity<JunglebookListResponse> {
        return ResponseEntity.ok(JunglebookListResponse.of(junglebookService.inProgressList()))
    }

    @Operation(summary = "정글북 페이징 목록")
    @GetMapping("/debate")
    fun pageableList(
        @Min(1) @RequestParam(name = "pageNo", defaultValue = "1") pageNo: Int
    ): ResponseEntity<JunglebookListResponse> {
        val pageSize = 20
        return ResponseEntity.ok(junglebookService.pageableList(pageNo - 1, pageSize))
    }

    @Operation(summary = "정글북 상세보기")
    @GetMapping("/debate/{id}")
    fun view(@Min(1) @PathVariable(name = "id") id: Long): ResponseEntity<JunglebookResponse?> {
        val junglebook = junglebookService.junglebook(id)
        return ResponseEntity.ok(junglebook?.let { JunglebookResponse.of(it) })
    }
}