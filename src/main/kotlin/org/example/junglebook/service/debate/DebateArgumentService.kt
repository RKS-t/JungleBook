package org.example.junglebook.service.debate

import org.example.junglebook.constant.JBConstants
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.entity.debate.DebateArgumentEntity
import org.example.junglebook.entity.debate.DebateVoteEntity
import org.example.junglebook.enums.ArgumentStance
import org.example.junglebook.enums.DebateReferenceType
import org.example.junglebook.enums.VoteType
import org.example.junglebook.repository.MemberRepository
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateFileRepository
import org.example.junglebook.repository.debate.DebateTopicRepository
import org.example.junglebook.repository.debate.DebateVoteRepository
import org.example.junglebook.service.fallacy.FallacyDetectionService
import org.example.junglebook.util.logger
import org.example.junglebook.web.dto.DebateArgumentListResponse
import org.example.junglebook.web.dto.DebateArgumentResponse
import org.example.junglebook.web.dto.DebateArgumentSimpleResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate



@Service
class DebateArgumentService(
    private val debateArgumentRepository: DebateArgumentRepository,
    private val debateFileRepository: DebateFileRepository,
    private val debateTopicRepository: DebateTopicRepository,
    private val debateTopicService: DebateTopicService,
    private val fallacyDetectionService: FallacyDetectionService,
    private val transactionTemplate: TransactionTemplate
) {

    @Transactional(readOnly = true)
    fun getPopularList(topicId: Long): Map<ArgumentStance, List<DebateArgumentSimpleResponse>> {
        return ArgumentStance.values().associateWith { stance ->
            val arguments = debateArgumentRepository.findPopularByStance(topicId, stance)
            DebateArgumentSimpleResponse.of(arguments.take(JBConstants.DEBATE_POPULAR_ARGUMENTS_LIMIT))
        }
    }

    @Transactional(readOnly = true)
    fun getPageableList(topicId: Long, stance: ArgumentStance, pageNo: Int, limit: Int): DebateArgumentListResponse {
        val totalCount = debateArgumentRepository.countByTopicIdAndStanceAndActiveYnTrue(topicId, stance)
        val pageable = PageRequest.of(pageNo, limit)
        val list = debateArgumentRepository.findByTopicIdAndStanceAndActiveYnTrueOrderByCreatedAtDesc(
            topicId, stance, pageable
        )

        return DebateArgumentListResponse.of(totalCount.toInt(), pageNo, list)
    }

    @Transactional(readOnly = true)
    fun getArgument(topicId: Long, id: Long, increaseView: Boolean = false): DebateArgumentResponse? {
        val argument = debateArgumentRepository.findByIdAndTopicIdAndActiveYnTrue(id, topicId) ?: return null
        
        if (increaseView) {
            debateArgumentRepository.increaseViewCount(id)
        }
        
        return DebateArgumentResponse.of(argument)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun increaseViewCount(id: Long) {
        debateArgumentRepository.increaseViewCount(id)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun createArgument(entity: DebateArgumentEntity, fileIds: List<Long>?): DebateArgumentResponse {
        if (entity.content.length > JBConstants.DEBATE_ARGUMENT_MAX_CONTENT_LENGTH) {
            logger().warn("Argument content length exceeded: {} characters (max: {})", entity.content.length, JBConstants.DEBATE_ARGUMENT_MAX_CONTENT_LENGTH)
            throw GlobalException(
                DefaultErrorCode.WRONG_ACCESS,
                "논증 내용은 최대 ${JBConstants.DEBATE_ARGUMENT_MAX_CONTENT_LENGTH}자까지 작성할 수 있습니다. (현재: ${entity.content.length}자)"
            )
        }
        
        val savedEntity = debateArgumentRepository.save(entity)
        val savedEntityId = requireNotNull(savedEntity.id) { "Saved argument ID must not be null" }

        fileIds?.forEach { fileId ->
            debateFileRepository.updateAttachStatus(
                refType = DebateReferenceType.ARGUMENT.value,
                refId = savedEntityId,
                id = fileId,
                userId = savedEntity.userId
            )
        }

        debateTopicService.increaseArgumentCount(entity.topicId)

        // 논증의 부모 토픽 정보를 조회하여 컨텍스트에 포함
        val topic = debateTopicRepository.findByIdAndActiveYnTrue(entity.topicId)

        fallacyDetectionService.detectFallacyAsync(
            text = savedEntity.content,
            language = "ko",
            topicTitle = topic?.title,
            topicDescription = topic?.description
        ).thenAccept { result ->
                result?.let { fallacyResult ->
                    val argumentId = savedEntityId
                    try {
                        transactionTemplate.executeWithoutResult { status ->
                            debateArgumentRepository.findById(argumentId).ifPresent { entity ->
                                entity.fallacyHasFallacy = fallacyResult.hasFallacy
                                entity.fallacyType = fallacyResult.fallacyType
                                entity.fallacyConfidence = fallacyResult.confidence
                                entity.fallacyExplanation = fallacyResult.explanation
                                entity.fallacyCheckedYn = true
                                debateArgumentRepository.save(entity)
                                logger().info("Fallacy detection result saved: argumentId=$argumentId, hasFallacy=${fallacyResult.hasFallacy}, type=${fallacyResult.fallacyType}")
                            } ?: logger().warn("Argument not found for fallacy update: argumentId=$argumentId")
                        }
                    } catch (e: Exception) {
                        logger().error("Failed to save fallacy detection result: argumentId=$argumentId", e)
                    }
                }
            }.exceptionally { throwable ->
                logger().error("Failed to save fallacy detection result: argumentId=$savedEntityId", throwable)
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

        debateTopicService.decreaseArgumentCount(topicId)
    }

    @Transactional(readOnly = true)
    fun getArgumentsByAuthor(userId: Long, pageNo: Int, limit: Int): DebateArgumentListResponse {
        val pageable = PageRequest.of(pageNo, limit)
        val list = debateArgumentRepository.findByUserIdAndActiveYnTrueOrderByCreatedAtDesc(userId, pageable)
        val totalCount = debateArgumentRepository.countByUserIdAndActiveYnTrue(userId)

        return DebateArgumentListResponse.of(totalCount.toInt(), pageNo, list)
    }

    @Transactional(readOnly = true)
    fun getTopicStatistics(topicId: Long): Map<ArgumentStance, Int> {
        val statistics = debateArgumentRepository.countByTopicIdGroupByStance(topicId)
        return statistics.associate {
            (it[0] as ArgumentStance) to (it[1] as Long).toInt()
        }
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

    @Transactional(readOnly = true)
    fun getAllArgumentsByTopic(topicId: Long, pageNo: Int, limit: Int): DebateArgumentListResponse {
        val totalCount = debateArgumentRepository.countByTopicIdAndActiveYnTrue(topicId)
        val pageable = PageRequest.of(pageNo, limit)
        val list = debateArgumentRepository.findByTopicIdAndActiveYnTrueOrderByCreatedAtDesc(topicId, pageable)

        return DebateArgumentListResponse.of(totalCount.toInt(), pageNo, list)
    }
}