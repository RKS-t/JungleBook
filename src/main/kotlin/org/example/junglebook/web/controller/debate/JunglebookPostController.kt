package org.example.junglebook.web.controller.debate

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import jakarta.validation.constraints.Min
import org.example.junglebook.service.debate.JunglebookPostService
import org.example.junglebook.web.dto.JunglebookPostListResponse
import org.example.junglebook.web.dto.JunglebookPostResponse
import org.example.junglebook.web.dto.JunglebookPostSimpleResponse

@Tag(name = "정글북 토론 게시물")
@Validated
@RestController
@RequestMapping("/api/debate/{junglebookId}")
class JunglebookPostController(
    private val junglebookPostService: JunglebookPostService
) {

    @Operation(summary = "정글북 인기게시물")
    @GetMapping("/popular")
    fun popularList(
        @Min(1) @PathVariable(name = "junglebookId") junglebookId: Int
    ): ResponseEntity<Map<Int, List<JunglebookPostSimpleResponse>>> {
        return ResponseEntity.ok(junglebookPostService.popularList(junglebookId))
    }

    @Operation(summary = "캠프별 정글북 게시물 페이징 목록")
    @GetMapping("/posts")
    fun pageableList(
        @Min(1) @PathVariable(name = "junglebookId") junglebookId: Int,
        @Min(0) @RequestParam(name = "camp", defaultValue = "0") camp: Int,
        @Min(1) @RequestParam(name = "pageNo", defaultValue = "1") pageNo: Int
    ): ResponseEntity<JunglebookPostListResponse> {
        val pageSize = 20
        return ResponseEntity.ok(junglebookPostService.pageableList(junglebookId, camp, pageNo - 1, pageSize))
    }

    @Operation(summary = "정글북 게시물 상세보기")
    @GetMapping("/post/{id}")
    fun view(
        @Min(1) @PathVariable(name = "junglebookId") junglebookId: Int,
        @Min(1) @PathVariable(name = "id") id: Long
    ): ResponseEntity<JunglebookPostResponse?> {
        return ResponseEntity.ok(junglebookPostService.view(junglebookId, id))
    }
}