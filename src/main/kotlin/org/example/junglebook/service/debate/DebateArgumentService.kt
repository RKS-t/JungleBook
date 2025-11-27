package org.example.junglebook.service.debate

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
import org.example.junglebook.repository.debate.DebateVoteRepository
import org.example.junglebook.service.fallacy.FallacyDetectionService
import org.example.junglebook.web.dto.DebateArgumentListResponse
import org.example.junglebook.web.dto.DebateArgumentResponse
import org.example.junglebook.web.dto.DebateArgumentSimpleResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional



@Service
class DebateArgumentService(
    private val debateArgumentRepository: DebateArgumentRepository,
    private val debateFileRepository: DebateFileRepository,
    private val debateTopicService: DebateTopicService,
    private val fallacyDetectionService: FallacyDetectionService
) {

    /**
     * 1. 인기 논증 목록 (입장별)
     */
    @Transactional(readOnly = true)
    fun popularList(topicId: Long): Map<ArgumentStance, List<DebateArgumentSimpleResponse>> {
        val popular = mutableMapOf<ArgumentStance, List<DebateArgumentSimpleResponse>>()

        ArgumentStance.values().forEach { stance ->
            val arguments = debateArgumentRepository.findPopularByStance(topicId, stance)
            popular[stance] = DebateArgumentSimpleResponse.of(arguments.take(5)) // 상위 5개만
        }

        return popular
    }

    /**
     * 2. 입장별 논증 페이징 목록
     */
    @Transactional(readOnly = true)
    fun pageableList(topicId: Long, stance: ArgumentStance, pageNo: Int, limit: Int): DebateArgumentListResponse {
        val totalCount = debateArgumentRepository.countByTopicIdAndStanceAndActiveYnTrue(topicId, stance)
        val pageable = PageRequest.of(pageNo, limit)
        val list = debateArgumentRepository.findByTopicIdAndStanceAndActiveYnTrueOrderByCreatedAtDesc(
            topicId, stance, pageable
        )

        return DebateArgumentListResponse.of(totalCount.toInt(), pageNo, list)
    }

    /**
     * 3. 논증 상세 조회
     */
    @Transactional(readOnly = true)
    fun view(topicId: Long, id: Long): DebateArgumentResponse? {
        val argument = debateArgumentRepository.findByIdAndTopicIdAndActiveYnTrue(id, topicId)
        return argument?.let { DebateArgumentResponse.of(it) }
    }

    /**
     * 4. 조회수 증가
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun increaseViewCount(id: Long) {
        debateArgumentRepository.increaseViewCount(id)
    }

    /**
     * 5. 논증 생성
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun createArgument(entity: DebateArgumentEntity, fileIds: List<Long>?): DebateArgumentResponse {
        // 글자 수 제한 검증 (이중 체크)
        val maxContentLength = 5000
        if (entity.content.length > maxContentLength) {
            throw GlobalException(
                DefaultErrorCode.WRONG_ACCESS,
                "논증 내용은 최대 ${maxContentLength}자까지 작성할 수 있습니다. (현재: ${entity.content.length}자)"
            )
        }
        
        // 논증 저장
        val savedEntity = debateArgumentRepository.save(entity)

        // 파일 첨부 처리
        fileIds?.forEach { fileId ->
            debateFileRepository.updateAttachStatus(
                refType = DebateReferenceType.ARGUMENT.value,
                refId = savedEntity.id!!,
                id = fileId,
                userId = savedEntity.userId
            )
        }

        // 토픽 논증 수 증가
        debateTopicService.increaseArgumentCount(entity.topicId)

        // 토픽 정보 조회
        val topic = debateTopicService.getTopicDetail(entity.topicId, increaseView = false)

        // 논리 오류 탐지 (비동기)
        fallacyDetectionService.detectFallacyAsync(
            text = savedEntity.content,
            language = "ko",
            topicTitle = topic?.topic?.title,
            topicDescription = topic?.topic?.description
        ).thenAccept { result ->
                result?.let {
                    savedEntity.apply {
                        fallacyHasFallacy = it.hasFallacy
                        fallacyType = it.fallacyType
                        fallacyConfidence = it.confidence
                        fallacyExplanation = it.explanation
                        fallacyCheckedYn = true
                    }
                    debateArgumentRepository.save(savedEntity)
                }
            }

        return DebateArgumentResponse.of(savedEntity)
    }

    /**
     * 7. 논증 삭제 (Soft Delete)
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun deleteArgument(topicId: Long, id: Long, userId: Long): Boolean {
        val argument = debateArgumentRepository.findByIdAndTopicIdAndActiveYnTrue(id, topicId)
            ?: return false

        // 권한 검증
        if (argument.userId != userId) {
            throw GlobalException(DefaultErrorCode.DEBATE_ARGUMENT_DELETE_DENIED)
        }

        // Soft Delete
        val result = debateArgumentRepository.softDelete(id, userId)

        if (result > 0) {
            // 토픽 논증 수 감소
            debateTopicService.decreaseArgumentCount(topicId)
            return true
        }

        return false
    }

    /**
     * 8. 작성자별 논증 조회
     */
    @Transactional(readOnly = true)
    fun getArgumentsByAuthor(userId: Long, pageNo: Int, limit: Int): DebateArgumentListResponse {
        val pageable = PageRequest.of(pageNo, limit)
        val list = debateArgumentRepository.findByUserIdAndActiveYnTrueOrderByCreatedAtDesc(userId, pageable)
        val totalCount = debateArgumentRepository.countByUserIdAndActiveYnTrue(userId)

        return DebateArgumentListResponse.of(totalCount.toInt(), pageNo, list)
    }

    /**
     * 9. 토픽별 입장 통계
     */
    @Transactional(readOnly = true)
    fun getTopicStatistics(topicId: Long): Map<ArgumentStance, Int> {
        val statistics = debateArgumentRepository.countByTopicIdGroupByStance(topicId)
        return statistics.associate {
            (it[0] as ArgumentStance) to (it[1] as Long).toInt()
        }
    }

    /**
     * 10. 지지 토글
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun toggleSupport(id: Long, increase: Boolean): Boolean {
        val result = if (increase) {
            debateArgumentRepository.increaseSupportCount(id)
        } else {
            debateArgumentRepository.decreaseSupportCount(id)
        }
        return result > 0
    }

    /**
     * 11. 반대 토글
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun toggleOppose(id: Long, increase: Boolean): Boolean {
        val result = if (increase) {
            debateArgumentRepository.increaseOpposeCount(id)
        } else {
            debateArgumentRepository.decreaseOpposeCount(id)
        }
        return result > 0
    }

    /**
     * 12. 토픽의 전체 논증 목록 (입장 구분 없이)
     */
    @Transactional(readOnly = true)
    fun getAllArgumentsByTopic(topicId: Long, pageNo: Int, limit: Int): DebateArgumentListResponse {
        val totalCount = debateArgumentRepository.countByTopicIdAndActiveYnTrue(topicId)
        val pageable = PageRequest.of(pageNo, limit)
        val list = debateArgumentRepository.findByTopicIdAndActiveYnTrueOrderByCreatedAtDesc(topicId, pageable)

        return DebateArgumentListResponse.of(totalCount.toInt(), pageNo, list)
    }
}