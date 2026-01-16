package org.example.junglebook.web.controller.post

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.junglebook.enums.post.CountType
import org.example.junglebook.model.Member
import org.example.junglebook.service.MemberService
import org.example.junglebook.service.post.PostCommandService
import org.example.junglebook.service.post.PostQueryService
import org.example.junglebook.web.dto.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "게시판 게시글")
@Validated
@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postCommandService: PostCommandService,
    private val postQueryService: PostQueryService,
    private val memberService: MemberService
) {

    @Operation(summary = "게시글 생성")
    @PostMapping
    fun createPost(
        @AuthenticationPrincipal member: Member,
        @RequestParam boardId: Int,
        @RequestBody request: PostCreateRequest
    ): ResponseEntity<PostResponse> {
        val memberId = getMemberId(member)
        val postResponse = postCommandService.createPost(boardId, request, memberId)
        return ResponseEntity.status(HttpStatus.CREATED).body(postResponse)
    }

    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{postId}")
    fun getPostDetail(
        @PathVariable postId: Long,
        @RequestParam(defaultValue = "true") increaseView: Boolean
    ): ResponseEntity<PostDetailResponse> {
        val postDetail = postQueryService.getPostDetail(postId)
            ?: return ResponseEntity.notFound().build()
        if (increaseView) {
            postCommandService.increaseViewCount(postId)
        }
        return ResponseEntity.ok(postDetail)
    }

    @Operation(summary = "게시글 목록 조회")
    @GetMapping
    fun getPostList(
        @RequestParam boardId: Int,
        @RequestParam(defaultValue = "LATEST") sortType: PostSortType,
        @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) keyword: String?
    ): ResponseEntity<PostListResponse> {
        val postList = postQueryService.getPostList(boardId, sortType, pageNo, limit, keyword)
        return ResponseEntity.ok(postList)
    }

    @Operation(summary = "인기 게시글 조회")
    @GetMapping("/popular")
    fun getPopularPosts(
        @RequestParam boardId: Int,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<PostSimpleResponse>> {
        val posts = postQueryService.getPopularPosts(boardId, limit)
        return ResponseEntity.ok(posts)
    }

    @Operation(summary = "작성자별 게시글 조회")
    @GetMapping("/author/{userId}")
    fun getPostsByAuthor(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<PostListResponse> {
        val postList = postQueryService.getPostsByAuthor(userId, pageNo, limit)
        return ResponseEntity.ok(postList)
    }

    @Operation(summary = "게시글 수정")
    @PutMapping("/{postId}")
    fun updatePost(
        @AuthenticationPrincipal member: Member,
        @PathVariable postId: Long,
        @RequestBody request: PostUpdateRequest
    ): ResponseEntity<PostResponse> {
        val memberId = getMemberId(member)
        val postResponse = postCommandService.updatePost(postId, request, memberId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(postResponse)
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{postId}")
    fun deletePost(
        @AuthenticationPrincipal member: Member,
        @PathVariable postId: Long
    ): ResponseEntity<Void> {
        val memberId = getMemberId(member)
        postCommandService.deletePost(postId, memberId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "조회수 증가")
    @PostMapping("/{postId}/view")
    fun increaseViewCount(
        @PathVariable postId: Long
    ): ResponseEntity<Void> {
        postCommandService.increaseViewCount(postId)
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "좋아요 증가")
    @PostMapping("/{postId}/like")
    fun increaseLike(
        @AuthenticationPrincipal member: Member,
        @PathVariable postId: Long,
        @RequestParam boardId: Int
    ): ResponseEntity<CountResponse> {
        val memberId = getMemberId(member)
        val count = postCommandService.increaseCount(boardId, postId, memberId, CountType.LIKE)
        return ResponseEntity.ok(CountResponse(count))
    }

    private fun getMemberId(member: Member): Long {
        return memberService.getMemberId(member)
    }
}

