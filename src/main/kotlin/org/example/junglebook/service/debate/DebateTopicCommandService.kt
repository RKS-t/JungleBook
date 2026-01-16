package org.example.junglebook.service.debate

import org.example.junglebook.constant.JBConstants
import org.example.junglebook.enums.DebateTopicStatus
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.repository.debate.DebateTopicRepository
import org.example.junglebook.util.logger
import org.example.junglebook.web.dto.DebateTopicCreateRequest
import org.example.junglebook.web.dto.DebateTopicResponse
import org.example.junglebook.web.dto.DebateTopicUpdateRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class DebateTopicCommandService(
    private val debateTopicRepository: DebateTopicRepository
) {

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun createTopic(request: DebateTopicCreateRequest, creatorId: Long): DebateTopicResponse {
        val entity = request.toEntity(creatorId)
        val saved = debateTopicRepository.save(entity)
        return DebateTopicResponse.of(saved)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun updateTopic(topicId: Long, request: DebateTopicUpdateRequest, userId: Long): DebateTopicResponse? {
        val topic = debateTopicRepository.findByIdAndActiveYnTrue(topicId) ?: return null

        if (topic.creatorId != userId) {
            logger().warn("Unauthorized topic update attempt: topicId: {}, userId: {}, topicCreatorId: {}", topicId, userId, topic.creatorId)
            throw GlobalException(DefaultErrorCode.DEBATE_TOPIC_MODIFY_DENIED)
        }

        val updated = topic.copy(
            title = request.title ?: topic.title,
            description = request.description ?: topic.description,
            descriptionHtml = request.descriptionHtml ?: topic.descriptionHtml,
            category = request.category ?: topic.category,
            status = request.status ?: topic.status,
            hotYn = request.hotYn ?: topic.hotYn,
            startDate = request.startDate ?: topic.startDate,
            endDate = request.endDate ?: topic.endDate,
            updatedAt = LocalDateTime.now()
        )

        val saved = debateTopicRepository.save(updated)
        return DebateTopicResponse.of(saved)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun deleteTopic(topicId: Long, userId: Long) {
        val topic = debateTopicRepository.findByIdAndActiveYnTrue(topicId)
            ?: run {
                logger().warn("Topic not found for delete: topicId={}", topicId)
                throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "토픽을 찾을 수 없습니다.")
            }

        if (topic.creatorId != userId) {
            logger().warn("Unauthorized topic delete attempt: topicId: {}, userId: {}, topicCreatorId: {}", topicId, userId, topic.creatorId)
            throw GlobalException(DefaultErrorCode.DEBATE_TOPIC_DELETE_DENIED)
        }

        val deleted = topic.copy(activeYn = false, updatedAt = LocalDateTime.now())
        debateTopicRepository.save(deleted)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun updateHotTopics(threshold: Int = JBConstants.DEBATE_HOT_TOPIC_THRESHOLD) {
        val allTopics = debateTopicRepository.findAll()
            .filter { it.activeYn }

        val topicsToUpdate = allTopics.mapNotNull { topic ->
            val score = topic.argumentCount + (topic.viewCount / JBConstants.DEBATE_HOT_TOPIC_VIEW_COUNT_DIVISOR)
            val shouldBeHot = score >= threshold

            if (topic.hotYn != shouldBeHot) {
                topic.copy(
                    hotYn = shouldBeHot,
                    updatedAt = LocalDateTime.now()
                )
            } else {
                null
            }
        }

        if (topicsToUpdate.isNotEmpty()) {
            debateTopicRepository.saveAll(topicsToUpdate)
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun changeTopicStatus(topicId: Long, status: DebateTopicStatus, userId: Long): DebateTopicResponse? {
        val topic = debateTopicRepository.findByIdAndActiveYnTrue(topicId) ?: return null

        if (topic.creatorId != userId) {
            logger().warn("Unauthorized topic status change attempt: topicId: {}, userId: {}, topicCreatorId: {}", topicId, userId, topic.creatorId)
            throw GlobalException(DefaultErrorCode.DEBATE_TOPIC_STATUS_CHANGE_DENIED)
        }

        val updated = topic.copy(
            status = status,
            updatedAt = LocalDateTime.now()
        )

        val saved = debateTopicRepository.save(updated)
        return DebateTopicResponse.of(saved)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun increaseArgumentCount(topicId: Long) {
        debateTopicRepository.increaseArgumentCount(topicId)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun decreaseArgumentCount(topicId: Long) {
        debateTopicRepository.decreaseArgumentCount(topicId)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun increaseViewCount(topicId: Long) {
        debateTopicRepository.increaseViewCount(topicId)
    }
}
