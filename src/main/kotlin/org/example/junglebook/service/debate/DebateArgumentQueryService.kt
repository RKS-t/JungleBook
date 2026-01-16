package org.example.junglebook.service.debate

import org.example.junglebook.constant.JBConstants
import org.example.junglebook.enums.ArgumentStance
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.web.dto.DebateArgumentListResponse
import org.example.junglebook.web.dto.DebateArgumentResponse
import org.example.junglebook.web.dto.DebateArgumentSimpleResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DebateArgumentQueryService(
    private val debateArgumentRepository: DebateArgumentRepository
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
    fun getArgument(topicId: Long, id: Long): DebateArgumentResponse? {
        val argument = debateArgumentRepository.findByIdAndTopicIdAndActiveYnTrue(id, topicId) ?: return null
        return DebateArgumentResponse.of(argument)
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

    @Transactional(readOnly = true)
    fun getAllArgumentsByTopic(topicId: Long, pageNo: Int, limit: Int): DebateArgumentListResponse {
        val totalCount = debateArgumentRepository.countByTopicIdAndActiveYnTrue(topicId)
        val pageable = PageRequest.of(pageNo, limit)
        val list = debateArgumentRepository.findByTopicIdAndActiveYnTrueOrderByCreatedAtDesc(topicId, pageable)

        return DebateArgumentListResponse.of(totalCount.toInt(), pageNo, list)
    }
}
