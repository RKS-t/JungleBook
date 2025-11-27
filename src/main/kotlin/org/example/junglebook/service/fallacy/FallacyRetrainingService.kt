package org.example.junglebook.service.fallacy

import org.example.junglebook.entity.debate.DebateArgumentFallacyTrainingDataEntity
import org.example.junglebook.repository.debate.DebateArgumentFallacyAppealRepository
import org.example.junglebook.repository.debate.DebateArgumentFallacyTrainingDataRepository
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.service.fallacy.FallacyAppealService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class FallacyRetrainingService(
    private val trainingDataRepository: DebateArgumentFallacyTrainingDataRepository,
    private val appealRepository: DebateArgumentFallacyAppealRepository,
    private val argumentRepository: DebateArgumentRepository,
    private val fallacyDetectionService: FallacyDetectionService,
    private val fallacyAppealService: FallacyAppealService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(FallacyRetrainingService::class.java)
    }

    @Value("\${fallacy.detection.appeal.threshold:100}")
    private val retrainThreshold: Int = 100

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun collectTrainingData() {
        val appealedArguments = fallacyAppealService.getAppealedArgumentsForRetraining()
        
        logger.info("Collecting training data from ${appealedArguments.size} appealed arguments")
        
        appealedArguments.forEach { appeal ->
            val argument = argumentRepository.findById(appeal.argumentId).orElse(null)
                ?: return@forEach

            // 이미 생성된 학습 데이터가 있는지 확인
            val existingData = trainingDataRepository.findByArgumentId(argument.id)
            if (existingData.isEmpty()) {
                // 재학습 데이터 생성
                val actualLabel = determineCorrectLabel(argument, appeal)
                
                val trainingData = DebateArgumentFallacyTrainingDataEntity(
                    argumentId = argument.id,
                    text = argument.content,
                    label = actualLabel,
                    source = DebateArgumentFallacyTrainingDataEntity.SOURCE_USER_APPEAL,
                    usedForTraining = false
                )
                
                trainingDataRepository.save(trainingData)
                logger.info("Training data collected: argumentId=${argument.id}, label=$actualLabel")
            }
        }
    }

    fun getUnusedTrainingData(): List<DebateArgumentFallacyTrainingDataEntity> {
        return trainingDataRepository.findUnusedTrainingData()
    }

    fun countUnusedTrainingData(): Long {
        return trainingDataRepository.countUnusedTrainingData()
    }

    fun triggerRetraining() {
        logger.info("Triggering retraining...")
        
        // 1. 재학습 데이터 수집
        collectTrainingData()
        
        // 2. 미사용 학습 데이터 확인
        val unusedData = getUnusedTrainingData()
        if (unusedData.isEmpty()) {
            logger.warn("No unused training data available for retraining")
            return
        }
        
        logger.info("Found ${unusedData.size} unused training data samples")
        
        // 3. Python 서비스에 재학습 요청
        val trainingDataList = unusedData.map { data ->
            mapOf(
                "text" to data.text,
                "label" to data.label
            )
        }
        
        val retrainingResult = triggerPythonRetraining(trainingDataList)
        
        if (retrainingResult) {
            // 4. 사용된 데이터로 표시
            markDataAsUsed(unusedData)
            logger.info("Retraining triggered successfully")
        } else {
            logger.warn("Retraining failed, data not marked as used")
        }
    }

    private fun triggerPythonRetraining(trainingData: List<Map<String, String>>): Boolean {
        return try {
            val serviceUrl = "http://localhost:8000/api/v1"
            val url = "$serviceUrl/retrain"
            val request = mapOf("training_data" to trainingData)
            
            val restTemplate = org.springframework.web.client.RestTemplate()
            val response = restTemplate.postForObject(
                url,
                request,
                Map::class.java
            )
            
            val status = response?.get("status") as? String
            status == "success"
        } catch (e: Exception) {
            logger.error("Failed to trigger Python retraining: ${e.message}", e)
            false
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    private fun markDataAsUsed(data: List<DebateArgumentFallacyTrainingDataEntity>) {
        data.forEach { trainingData ->
            trainingData.usedForTraining = true
            trainingDataRepository.save(trainingData)
        }
    }

    private fun determineCorrectLabel(
        argument: org.example.junglebook.entity.debate.DebateArgumentEntity,
        appeal: org.example.junglebook.entity.debate.DebateArgumentFallacyAppealEntity
    ): String {
        // AI가 오류가 있다고 판단했는데 의의가 제기된 경우 -> no_fallacy
        return if (argument.fallacyHasFallacy == true) {
            "no_fallacy"
        } else {
            // 실제 오류 타입 추론 (의의 제기 이유 분석)
            // 간단한 구현: 의의 제기 이유에서 키워드 매칭
            val reason = appeal.appealReason.lowercase()
            when {
                reason.contains("인신공격") || reason.contains("ad hominem") -> "ad_hominem"
                reason.contains("허수아비") || reason.contains("straw man") -> "straw_man"
                reason.contains("양자택일") || reason.contains("false dilemma") -> "false_dilemma"
                reason.contains("감정") || reason.contains("emotion") -> "appeal_to_emotion"
                reason.contains("순환") || reason.contains("circular") -> "circular_reasoning"
                reason.contains("성급한") || reason.contains("hasty") -> "hasty_generalization"
                reason.contains("인과") || reason.contains("cause") -> "false_cause"
                reason.contains("다수") || reason.contains("bandwagon") -> "bandwagon"
                reason.contains("권위") || reason.contains("authority") -> "appeal_to_authority"
                reason.contains("빨간") || reason.contains("red herring") -> "red_herring"
                else -> "no_fallacy"
            }
        }
    }

    fun checkRetrainingThreshold() {
        val appealedArguments = fallacyAppealService.getAppealedArgumentsForRetraining()
        
        if (appealedArguments.isNotEmpty()) {
            logger.info("Retraining threshold reached: ${appealedArguments.size} arguments need review")
            triggerRetraining()
        }
    }
}

