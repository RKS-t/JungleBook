package org.example.junglebook.service.debate

import org.example.junglebook.constant.JBConstants
import org.example.junglebook.enums.DebateReferenceType
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateFileRepository
import org.example.junglebook.repository.debate.DebateTopicRepository
import org.example.junglebook.service.fallacy.FallacyDetectionService
import org.example.junglebook.util.logger
import org.example.junglebook.web.dto.DebateArgumentCreateRequest
import org.example.junglebook.web.dto.DebateArgumentResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Service
class DebateArgumentCommandService(
    private val debateArgumentRepository: DebateArgumentRepository,
    private val debateFileRepository: DebateFileRepository,
    private val debateTopicRepository: DebateTopicRepository,
    private val debateTopicCommandService: DebateTopicCommandService,
    private val fallacyDetectionService: FallacyDetectionService,
    private val transactionTemplate: TransactionTemplate
) {

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun createArgument(topicId: Long, userId: Long, request: DebateArgumentCreateRequest): DebateArgumentResponse {
        if (request.content.length > JBConstants.DEBATE_ARGUMENT_MAX_CONTENT_LENGTH) {
            logger().warn("Argument content length exceeded: {} characters (max: {})", request.content.length, JBConstants.DEBATE_ARGUMENT_MAX_CONTENT_LENGTH)
            throw GlobalException(
                DefaultErrorCode.WRONG_ACCESS,
                "논증 내용은 최대 ${JBConstants.DEBATE_ARGUMENT_MAX_CONTENT_LENGTH}자까지 작성할 수 있습니다. (현재: ${request.content.length}자)"
            )
        }

        val entity = request.toEntity(topicId, userId)
        val savedEntity = debateArgumentRepository.save(entity)
        val savedEntityId = requireNotNull(savedEntity.id) { "Saved argument ID must not be null" }

        request.fileIds?.forEach { fileId ->
            debateFileRepository.updateAttachStatus(
                refType = DebateReferenceType.ARGUMENT.value,
                refId = savedEntityId,
                id = fileId,
                userId = savedEntity.userId
            )
        }

        debateTopicCommandService.increaseArgumentCount(topicId)

        val topic = debateTopicRepository.findByIdAndActiveYnTrue(topicId)

        fallacyDetectionService.detectFallacyAsync(
            text = savedEntity.content,
            language = "ko",
            topicTitle = topic?.title,
            topicDescription = topic?.description
        ).thenAccept { result ->
            result?.let { fallacyResult ->
                val argumentId = savedEntityId
                try {
                    transactionTemplate.executeWithoutResult {
                        debateArgumentRepository.findById(argumentId).ifPresent { entityToUpdate ->
                            entityToUpdate.fallacyHasFallacy = fallacyResult.hasFallacy
                            entityToUpdate.fallacyType = fallacyResult.fallacyType
                            entityToUpdate.fallacyConfidence = fallacyResult.confidence
                            entityToUpdate.fallacyExplanation = fallacyResult.explanation
                            entityToUpdate.fallacyCheckedYn = true
                            debateArgumentRepository.save(entityToUpdate)
                            logger().info("Fallacy detection result saved: argumentId=$argumentId, hasFallacy=${fallacyResult.hasFallacy}, type=${fallacyResult.fallacyType}")
                        } ?: logger().warn("Argument not found for fallacy update: argumentId=$argumentId")
                    }
                } catch (e: Exception) {
                    logger().error("Failed to save fallacy detection result: argumentId=$argumentId", e)
                }
            }
        }.exceptionally { throwable ->
            when (throwable) {
                is java.util.concurrent.TimeoutException -> {
                    logger().warn("Fallacy detection timeout: argumentId=$savedEntityId, timeout=${fallacyDetectionService.getTimeout()}ms")
                }
                else -> {
                    logger().error("Failed to save fallacy detection result: argumentId=$savedEntityId", throwable)
                }
            }
            null
        }

        return DebateArgumentResponse.of(savedEntity)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun deleteArgument(topicId: Long, id: Long, userId: Long) {
        val argument = debateArgumentRepository.findByIdAndTopicIdAndActiveYnTrue(id, topicId)
            ?: throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "논증을 찾을 수 없습니다.")

        if (argument.userId != userId) {
            logger().warn("Unauthorized argument delete attempt: argumentId: {}, userId: {}, argumentOwnerId: {}", id, userId, argument.userId)
            throw GlobalException(DefaultErrorCode.DEBATE_ARGUMENT_DELETE_DENIED)
        }

        val result = debateArgumentRepository.softDelete(id, userId)

        if (result <= 0) {
            throw GlobalException(DefaultErrorCode.WRONG_ACCESS)
        }

        debateTopicCommandService.decreaseArgumentCount(topicId)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun increaseViewCount(id: Long) {
        debateArgumentRepository.increaseViewCount(id)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun toggleSupport(id: Long, increase: Boolean): Boolean {
        val result = if (increase) {
            debateArgumentRepository.increaseSupportCount(id)
        } else {
            debateArgumentRepository.decreaseSupportCount(id)
        }
        return result > 0
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun toggleOppose(id: Long, increase: Boolean): Boolean {
        val result = if (increase) {
            debateArgumentRepository.increaseOpposeCount(id)
        } else {
            debateArgumentRepository.decreaseOpposeCount(id)
        }
        return result > 0
    }
}
