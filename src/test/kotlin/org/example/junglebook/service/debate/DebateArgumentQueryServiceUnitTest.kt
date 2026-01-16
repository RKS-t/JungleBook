package org.example.junglebook.service.debate

import org.assertj.core.api.Assertions.assertThat
import org.example.junglebook.entity.debate.DebateArgumentEntity
import org.example.junglebook.enums.ArgumentStance
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class DebateArgumentQueryServiceUnitTest {

    @Mock
    private lateinit var debateArgumentRepository: DebateArgumentRepository

    @InjectMocks
    private lateinit var debateArgumentQueryService: DebateArgumentQueryService

    private lateinit var argumentEntity: DebateArgumentEntity

    @BeforeEach
    fun setUp() {
        argumentEntity = DebateArgumentEntity(
            id = 1L,
            topicId = 1L,
            userId = 100L,
            stance = ArgumentStance.PRO,
            title = "테스트 논증",
            content = "테스트 내용",
            authorNickname = "테스트유저",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `getArgument - 성공 케이스`() {
        val topicId = 1L
        val argumentId = 1L

        whenever(debateArgumentRepository.findByIdAndTopicIdAndActiveYnTrue(argumentId, topicId))
            .thenReturn(argumentEntity)

        val result = debateArgumentQueryService.getArgument(topicId, argumentId)

        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(argumentId)
    }

    @Test
    fun `getArgument - 논증을 찾을 수 없는 경우`() {
        val topicId = 1L
        val argumentId = 999L

        whenever(debateArgumentRepository.findByIdAndTopicIdAndActiveYnTrue(argumentId, topicId))
            .thenReturn(null)

        val result = debateArgumentQueryService.getArgument(topicId, argumentId)

        assertThat(result).isNull()
    }

    @Test
    fun `getPopularList - 성공 케이스`() {
        val topicId = 1L

        whenever(debateArgumentRepository.findPopularByStance(eq(topicId), any())).thenReturn(emptyList())

        val result = debateArgumentQueryService.getPopularList(topicId)

        assertThat(result).isNotNull
        assertThat(result.keys).containsAll(ArgumentStance.values().toList())
    }

    @Test
    fun `getPageableList - 성공 케이스`() {
        val topicId = 1L
        val stance = ArgumentStance.PRO
        val pageNo = 0
        val limit = 20

        whenever(debateArgumentRepository.countByTopicIdAndStanceAndActiveYnTrue(topicId, stance)).thenReturn(10L)
        whenever(debateArgumentRepository.findByTopicIdAndStanceAndActiveYnTrueOrderByCreatedAtDesc(any(), any(), any()))
            .thenReturn(listOf(argumentEntity))

        val result = debateArgumentQueryService.getPageableList(topicId, stance, pageNo, limit)

        assertThat(result).isNotNull
        assertThat(result.totalCount).isEqualTo(10)
    }
}
