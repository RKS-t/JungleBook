package org.example.junglebook.service.debate

import org.assertj.core.api.Assertions.assertThat
import org.example.junglebook.entity.MemberEntity
import org.example.junglebook.entity.debate.DebateArgumentEntity
import org.example.junglebook.enums.ArgumentStance
import org.example.junglebook.enums.VoteType
import org.example.junglebook.repository.MemberRepository
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateVoteRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class DebateVoteCommandServiceUnitTest {

    @Mock
    private lateinit var debateVoteRepository: DebateVoteRepository

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var debateArgumentRepository: DebateArgumentRepository

    @InjectMocks
    private lateinit var debateVoteCommandService: DebateVoteCommandService

    private lateinit var memberEntity: MemberEntity
    private lateinit var argumentEntity: DebateArgumentEntity

    @BeforeEach
    fun setUp() {
        memberEntity = MemberEntity(
            id = 100L,
            loginId = "user1",
            password = "password",
            name = "테스트유저",
            birth = "1990-01-01",
            phoneNumber = "01012345678",
            email = "test@example.com",
            sex = org.example.junglebook.enums.Sex.M,
            ideology = org.example.junglebook.enums.Ideology.M,
            nickname = "테스트유저",
            profileImage = "",
            deleteYn = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

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
    fun `vote - 성공 케이스`() {
        whenever(memberRepository.findById(100L)).thenReturn(Optional.of(memberEntity))
        whenever(debateArgumentRepository.findById(1L)).thenReturn(Optional.of(argumentEntity))
        whenever(debateVoteRepository.findByMemberIdAndArgumentIdAndVoteType(100L, 1L, VoteType.UPVOTE)).thenReturn(null)
        whenever(debateArgumentRepository.increaseSupportCount(1L)).thenReturn(1)

        val result = debateVoteCommandService.vote(1L, 100L, VoteType.UPVOTE)

        assertThat(result).isTrue()
    }
}
