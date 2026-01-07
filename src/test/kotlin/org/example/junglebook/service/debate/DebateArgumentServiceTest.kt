package org.example.junglebook.service.debate

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.example.junglebook.entity.MemberEntity
import org.example.junglebook.entity.debate.DebateArgumentEntity
import org.example.junglebook.entity.debate.DebateTopicEntity
import org.example.junglebook.enums.ArgumentStance
import org.example.junglebook.enums.DebateReferenceType
import org.example.junglebook.enums.DebateTopicCategory
import org.example.junglebook.enums.DebateTopicStatus
import org.example.junglebook.enums.Ideology
import org.example.junglebook.enums.Sex
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.repository.MemberRepository
import org.example.junglebook.repository.debate.DebateArgumentRepository
import org.example.junglebook.repository.debate.DebateFileRepository
import org.example.junglebook.repository.debate.DebateTopicRepository
import org.example.junglebook.service.JwtService
import org.example.junglebook.service.fallacy.FallacyDetectionService
import org.example.junglebook.web.dto.DebateArgumentResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class DebateArgumentServiceTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtService: JwtService

    @Autowired
    private lateinit var debateTopicRepository: DebateTopicRepository

    @Autowired
    private lateinit var debateArgumentRepository: DebateArgumentRepository

    private lateinit var ownerMember: MemberEntity
    private lateinit var otherMember: MemberEntity
    private lateinit var ownerToken: String
    private lateinit var otherToken: String

    private var topicId: Long = 0

    @BeforeEach
    fun setUpIntegration() {
        ownerMember = createMember("owner", "owner@test.com", "owner123")
        otherMember = createMember("other", "other@test.com", "other123")

        val ownerMemberModel = org.example.junglebook.model.Member.from(ownerMember)
        val otherMemberModel = org.example.junglebook.model.Member.from(otherMember)
        ownerToken = "Bearer ${jwtService.generateToken(ownerMemberModel, true)}"
        otherToken = "Bearer ${jwtService.generateToken(otherMemberModel, true)}"

        topicId = createTopic(ownerMember.id!!)
    }

    private fun createMember(loginId: String, email: String, nickname: String): MemberEntity {
        val member = MemberEntity(
            loginId = loginId,
            password = passwordEncoder.encode("Passw0rd!"),
            name = "Test User",
            phoneNumber = "01012345678",
            email = email,
            birth = "1990-01-01",
            nickname = nickname,
            sex = Sex.M,
            ideology = Ideology.M,
            profileImage = "",
            deleteYn = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        return memberRepository.save(member)
    }

    private fun createTopic(creatorId: Long): Long {
        val topic = DebateTopicEntity(
            title = "테스트 토픽",
            description = "테스트 설명",
            descriptionHtml = "<p>테스트 설명</p>",
            category = DebateTopicCategory.POLITICS,
            creatorId = creatorId,
            status = DebateTopicStatus.DEBATING,
            endDate = LocalDate.now().plusDays(30),
            activeYn = true
        )
        val saved = debateTopicRepository.save(topic)
        return saved.id!!
    }

    private fun createArgument(topicId: Long, userId: Long): Long {
        val argument = DebateArgumentEntity(
            topicId = topicId,
            userId = userId,
            title = "테스트 논증",
            content = "테스트 내용",
            contentHtml = "<p>테스트 내용</p>",
            stance = ArgumentStance.PRO,
            authorNickname = "테스터",
            activeYn = true
        )
        val saved = debateArgumentRepository.save(argument)
        return saved.id!!
    }

    @Test
    @DisplayName("논증 삭제 - 작성자만 가능")
    fun shouldDeleteArgument_whenOwner() {
        val newArgumentId = createArgument(topicId, ownerMember.id!!)

        // 작성자가 삭제하는 경우 - 성공
        mockMvc.perform(
            delete("/api/debate/topics/$topicId/arguments/$newArgumentId")
                .header("Authorization", ownerToken)
        )
            .andExpect(status().isNoContent)

        val anotherArgumentId = createArgument(topicId, ownerMember.id!!)

        // 다른 사용자가 삭제하려는 경우 - 실패 (403 Forbidden)
        val result = mockMvc.perform(
            delete("/api/debate/topics/$topicId/arguments/$anotherArgumentId")
                .header("Authorization", otherToken)
        )
            .andReturn()

        org.junit.jupiter.api.Assertions.assertTrue(
            result.response.status in listOf(403, 400),
            "예상: 403 Forbidden 또는 400 Bad Request, 실제: ${result.response.status}"
        )
        
        // 응답 본문에 에러 메시지가 포함되어 있는지 확인
        val responseBody = result.response.contentAsString
        org.junit.jupiter.api.Assertions.assertTrue(
            responseBody.contains("논증") || responseBody.contains("권한") || responseBody.contains("접근"),
            "응답 본문에 에러 메시지가 없습니다: $responseBody"
        )
    }
}
