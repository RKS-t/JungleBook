package org.example.junglebook.web.controller.debate

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "정글북 토론 댓글")
@Validated
@RestController
@RequestMapping("/api/debate/{id}/post/{postId}/reply")
class JunglebookPostReplyController {

    // 추후 댓글 관련 API들이 추가될 예정
}