package org.example.junglebook.web.controller.post
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import jakarta.validation.constraints.Min
import org.example.junglebook.entity.MemberEntity
import org.example.junglebook.enums.post.Board
import org.example.junglebook.service.post.PostCommandService
import org.example.junglebook.web.dto.PostFileResponse

@Tag(name = "게시판")
@Validated
@RestController
@RequestMapping("/api/board")
class BoardController(
    private val postCommandService: PostCommandService
) {

    @Operation(summary = "게시판 목록")
    @GetMapping
    fun boardList(): ResponseEntity<Array<Board>> {
        return ResponseEntity.ok(Board.values())
    }

    @Operation(summary = "파일 업로드")
    @PostMapping("/{boardName}/file-upload")
    fun fileUpload(
        @AuthenticationPrincipal member: MemberEntity,
        @Min(1) @PathVariable("boardName") boardName: String,
        @Parameter(description = "File", required = true) @RequestPart("file") file: MultipartFile
    ): ResponseEntity<PostFileResponse> {
        val boardId = Board.getIdByBoardName(boardName)
        val memberId = requireNotNull(member.id) { "Member ID must not be null" }
        return ResponseEntity.ok(
            PostFileResponse.of(
                postCommandService.insertPostFile(boardId, false, memberId, file)
            )
        )
    }
}