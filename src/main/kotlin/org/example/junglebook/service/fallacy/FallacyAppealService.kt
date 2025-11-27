package org.example.junglebook.service.fallacy

import org.example.junglebook.entity.debate.DebateArgumentEntity
import org.example.junglebook.entity.debate.DebateArgumentFallacyAppealEntity
import org.example.junglebook.entity.debate.DebateArgumentFallacyTrainingDataEntity
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateArgumentFallacyAppealRepository
import org.example.junglebook.repository.debate.DebateArgumentFallacyTrainingDataRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class FallacyAppealService(
    private val appealRepository: DebateArgumentFallacyAppealRepository,
    private val argumentRepository: DebateArgumentRepository,
    private val trainingDataRepository: DebateArgumentFallacyTrainingDataRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(FallacyAppealService::class.java)
    }

    @Value("\${fallacy.detection.appeal.threshold:100}")
    private val retrainThreshold: Int = 100

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun createAppeal(argumentId: Long, userId: Long, reason: String): DebateArgumentFallacyAppealEntity {
        val argument = argumentRepository.findById(argumentId)
            .filter { it.activeYn }
            .orElseThrow { GlobalException(DefaultErrorCode.WRONG_ACCESS, "논증을 찾을 수 없습니다.") }

        // AI의 원래 판단 저장
        val aiOriginalJudgment = buildString {
            append("오류 여부: ${argument.fallacyHasFallacy ?: false}\n")
            append("오류 타입: ${argument.fallacyType ?: "없음"}\n")
            append("신뢰도: ${argument.fallacyConfidence ?: 0.0}\n")
            append("설명: ${argument.fallacyExplanation ?: "없음"}")
        }

        val appeal = DebateArgumentFallacyAppealEntity(
            argumentId = argumentId,
            appealerId = userId,
            appealReason = reason,
            aiOriginalJudgment = aiOriginalJudgment,
            status = DebateArgumentFallacyAppealEntity.STATUS_PENDING
        )

        val savedAppeal = appealRepository.save(appeal)
        logger.info("Appeal created: argumentId=$argumentId, appealerId=$userId")

        // 재학습 임계값 확인
        checkRetrainingThreshold(argumentId)

        return savedAppeal
    }

    fun getAppealsByArgument(argumentId: Long): List<DebateArgumentFallacyAppealEntity> {
        return appealRepository.findByArgumentId(argumentId)
    }

    fun getPendingAppeals(): List<DebateArgumentFallacyAppealEntity> {
        return appealRepository.findByStatusOrderByCreatedAtDesc(
            DebateArgumentFallacyAppealEntity.STATUS_PENDING
        )
    }

    fun countAppealsByArgument(argumentId: Long): Int {
        return appealRepository.countByArgumentId(argumentId)
    }

    fun countPendingAppealsByArgument(argumentId: Long): Int {
        return appealRepository.countByArgumentIdAndStatus(
            argumentId,
            DebateArgumentFallacyAppealEntity.STATUS_PENDING
        )
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun approveAppeal(appealId: Long, adminId: Long): DebateArgumentFallacyAppealEntity {
        val appeal = appealRepository.findById(appealId)
            .orElseThrow { GlobalException(DefaultErrorCode.WRONG_ACCESS, "의의를 찾을 수 없습니다.") }

        if (!appeal.isPending()) {
            throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "이미 처리된 의의입니다.")
        }

        appeal.approve()
        val savedAppeal = appealRepository.save(appeal)

        // 재학습 데이터 생성
        createTrainingDataFromAppeal(appeal)

        logger.info("Appeal approved: appealId=$appealId, adminId=$adminId")
        return savedAppeal
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun rejectAppeal(appealId: Long, adminId: Long): DebateArgumentFallacyAppealEntity {
        val appeal = appealRepository.findById(appealId)
            .orElseThrow { GlobalException(DefaultErrorCode.WRONG_ACCESS, "의의를 찾을 수 없습니다.") }

        if (!appeal.isPending()) {
            throw GlobalException(DefaultErrorCode.WRONG_ACCESS, "이미 처리된 의의입니다.")
        }

        appeal.reject()
        val savedAppeal = appealRepository.save(appeal)

        logger.info("Appeal rejected: appealId=$appealId, adminId=$adminId")
        return savedAppeal
    }

    private fun checkRetrainingThreshold(argumentId: Long) {
        val pendingCount = countPendingAppealsByArgument(argumentId)
        if (pendingCount >= retrainThreshold) {
            logger.info("Retraining threshold reached: argumentId=$argumentId, count=$pendingCount")
            // 재학습 트리거는 별도 서비스에서 처리
        }
    }

    private fun createTrainingDataFromAppeal(appeal: DebateArgumentFallacyAppealEntity) {
        val argument = argumentRepository.findById(appeal.argumentId).orElse(null)
            ?: return

        // 실제 라벨 결정 (AI 판단과 반대 또는 다른 오류 타입)
        val actualLabel = determineActualLabel(argument, appeal)

        val trainingData = DebateArgumentFallacyTrainingDataEntity(
            argumentId = appeal.argumentId,
            text = argument.content,
            label = actualLabel,
            source = DebateArgumentFallacyTrainingDataEntity.SOURCE_USER_APPEAL,
            usedForTraining = false
        )

        trainingDataRepository.save(trainingData)
        logger.info("Training data created from appeal: appealId=${appeal.id}, label=$actualLabel")
    }

    private fun determineActualLabel(
        argument: DebateArgumentEntity,
        appeal: DebateArgumentFallacyAppealEntity
    ): String {
        // AI가 오류가 있다고 판단했는데 의의가 제기된 경우 -> no_fallacy
        // AI가 오류가 없다고 판단했는데 의의가 제기된 경우 -> 실제 오류 타입 추론 필요
        return if (argument.fallacyHasFallacy == true) {
            "no_fallacy"
        } else {
            // 의의 제기 이유에서 오류 타입 추론 (간단한 구현)
            // 실제로는 더 정교한 로직 필요
            "unknown_fallacy"
        }
    }

    fun getAppealedArgumentsForRetraining(): List<DebateArgumentFallacyAppealEntity> {
        return appealRepository.findAppealedArgumentsForRetraining(retrainThreshold)
    }
}

