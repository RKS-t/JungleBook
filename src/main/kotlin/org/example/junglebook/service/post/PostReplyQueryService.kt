package org.example.junglebook.service.post

import org.example.junglebook.repository.post.PostReplyRepository
import org.example.junglebook.web.dto.PostReplyResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostReplyQueryService(
    private val postReplyRepository: PostReplyRepository
) {

    @Transactional(readOnly = true)
    fun postReplyList(postId: Long): List<PostReplyResponse> {
        val pageable = PageRequest.of(0, 1000)
        val replies = postReplyRepository.findByPostIdAndUseYnTrueOrderByCreatedDtAsc(postId, pageable)
        return replies.map { PostReplyResponse.of(it) }
    }
}
