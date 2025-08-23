package org.example.junglebook.service.debate

import kr.co.minust.api.exception.GlobalException
import org.example.junglebook.entity.debate.JunglebookPostEntity
import org.example.junglebook.enums.post.CountType
import org.example.junglebook.enums.post.PostReferenceType
import org.example.junglebook.repository.debate.JunglebookCountHistoryRepository
import org.example.junglebook.repository.debate.JunglebookFileRepository
import org.example.junglebook.repository.debate.JunglebookPostRepository
import org.example.junglebook.web.dto.JunglebookPostListResponse
import org.example.junglebook.web.dto.JunglebookPostResponse
import org.example.junglebook.web.dto.JunglebookPostSimpleResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional


@Service
open class JunglebookPostService(
    private val junglebookPostRepository: JunglebookPostRepository,
    private val junglebookFileRepository: JunglebookFileRepository,
    private val junglebookCountHistoryRepository: JunglebookCountHistoryRepository
) {

    fun popularList(junglebookId: Int): Map<Int, List<JunglebookPostSimpleResponse>> {
        val popular = mutableMapOf<Int, List<JunglebookPostSimpleResponse>>()

        val leftPosts = junglebookPostRepository.findPopularByCamp(junglebookId, 1)
        popular[1] = JunglebookPostSimpleResponse.of(leftPosts)

        val rightPosts = junglebookPostRepository.findPopularByCamp(junglebookId, 2)
        popular[2] = JunglebookPostSimpleResponse.of(rightPosts)

        return popular
    }

    fun pageableList(junglebookId: Int, camp: Int, pageNo: Int, limit: Int): JunglebookPostListResponse {
        val totalCount = junglebookPostRepository.countByJunglebookIdAndCampAndUseYnTrue(junglebookId, camp)
        val pageable = PageRequest.of(pageNo, limit)
        val list = junglebookPostRepository.findByJunglebookIdAndCampAndUseYnTrueOrderByCreatedDtDesc(
            junglebookId, camp, pageable
        )

        return JunglebookPostListResponse.of(totalCount, pageNo, list)
    }

    fun view(junglebookId: Int, id: Long): JunglebookPostResponse? {
        val post = junglebookPostRepository.findByIdAndJunglebookIdAndUseYnTrue(id, junglebookId)
        return post?.let { JunglebookPostResponse.of(it) }
    }

    fun increaseViewCount(id: Long) {
        junglebookPostRepository.increaseViewCount(id)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    open fun increaseCount(junglebookId: Int, id: Long, userId: Long, countType: CountType): Boolean {
        val existCount = 0 // 주석 처리된 부분 그대로 유지

        if (existCount > 0) {
            throw GlobalException("ALREADY_EXISTS")
        }

        val updateResult = when (countType) {
            CountType.LIKE -> junglebookPostRepository.increaseLikeCount(id)
            CountType.DISLIKE -> junglebookPostRepository.increaseDislikeCount(id)
            else -> throw GlobalException("WRONG_ACCESS")
        }

        if (updateResult > 0) {
            // 주석 처리된 히스토리 저장 부분 그대로 유지
            return when (countType) {
                CountType.LIKE, CountType.DISLIKE -> true
                else -> throw GlobalException("WRONG_ACCESS")
            }
        } else {
            throw GlobalException("WRONG_ACCESS")
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    open fun createPost(entity: JunglebookPostEntity, fileIds: List<Long>?) {
        val savedEntity = junglebookPostRepository.save(entity)

        fileIds?.forEach { fileId ->
            junglebookFileRepository.updateAttachStatus(
                refType = PostReferenceType.POST.value,
                refId = savedEntity.id,
                id = fileId,
                userId = savedEntity.userId
            )
        }
    }
}