package org.example.junglebook.service.debate

import kr.co.minust.api.exception.GlobalException
import org.example.junglebook.entity.debate.JunglebookCountHistoryEntity
import org.example.junglebook.entity.debate.JunglebookEntity
import org.example.junglebook.enums.debate.JunglebookReferenceType
import org.example.junglebook.enums.post.CountType
import org.example.junglebook.repository.debate.JunglebookCountHistoryRepository
import org.example.junglebook.repository.debate.JunglebookRepository
import org.example.junglebook.web.dto.JunglebookListResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional


@Service
class JunglebookService(
    private val junglebookRepository: JunglebookRepository,
    private val junglebookCountHistoryRepository: JunglebookCountHistoryRepository
) {

    fun hot(): JunglebookEntity? {
        return junglebookRepository.findHotOne()
    }

    fun inProgressList(): List<JunglebookEntity> {
        return junglebookRepository.findInProgressList()
    }

    fun pageableList(pageNo: Int, limit: Int): JunglebookListResponse {
        val totalCount = junglebookRepository.count().toInt()
        val pageable = PageRequest.of(pageNo, limit)
        val entities = junglebookRepository.findPageableList(pageable)

        return JunglebookListResponse.of(totalCount, pageNo, entities)
    }

    fun junglebook(id: Long): JunglebookEntity? {
        return junglebookRepository.findById(id).orElse(null)
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun increaseCount(id: Long, userId: Long): Int {
        val existCount = junglebookCountHistoryRepository.countByRefTypeAndRefIdAndUserId(
            JunglebookReferenceType.POST, id, userId
        )

        if (existCount > 0) {
            throw GlobalException("ALREADY_EXISTS")
        }

        val updateResult = junglebookRepository.increasePostCount(id)

        if (updateResult > 0) {
            val history = JunglebookCountHistoryEntity(
                refType = JunglebookReferenceType.JUNGLEBOOK,
                refId = id,
                userId = userId,
                type = CountType.POST
            )
            junglebookCountHistoryRepository.save(history)

            return junglebookRepository.findById(id).orElseThrow {
                GlobalException("WRONG_ACCESS")
            }.postCnt
        } else {
            throw GlobalException("WRONG_ACCESS")
        }
    }
}