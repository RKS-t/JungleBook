package org.example.junglebook.service.post

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.junglebook.entity.MemberEntity
import org.example.junglebook.entity.post.PostEntity
import org.example.junglebook.enums.Ideology
import org.example.junglebook.enums.Sex
import org.example.junglebook.enums.post.Board
import org.example.junglebook.repository.MemberRepository
import org.example.junglebook.repository.post.BoardRepository
import org.example.junglebook.repository.post.PostRepository
import org.example.junglebook.service.JwtService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PostServiceTest {

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
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var boardRepository: BoardRepository

    private lateinit var ownerMember: MemberEntity
    private lateinit var otherMember: MemberEntity
    private lateinit var ownerToken: String
    private lateinit var otherToken: String

    private var postId: Long = 0

    @BeforeEach
    fun setUp() {
        ownerMember = createMember("owner", "owner@test.com", "owner123")
        otherMember = createMember("other", "other@test.com", "other123")

        val ownerMemberModel = org.example.junglebook.model.Member.from(ownerMember)
        val otherMemberModel = org.example.junglebook.model.Member.from(otherMember)
        ownerToken = "Bearer ${jwtService.generateToken(ownerMemberModel, true)}"
        otherToken = "Bearer ${jwtService.generateToken(otherMemberModel, true)}"

        postId = createPost(ownerMember.id!!)
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

    private fun createPost(userId: Long): Long {
        // Board.FREE.id는 2이므로, BoardEntity가 존재하는지 확인하고 없으면 생성
        val boardId = Board.FREE.id
        if (!boardRepository.existsById(boardId)) {
            // BoardEntity의 id는 자동 생성되므로, type과 name을 사용해서 생성
            val board = org.example.junglebook.entity.post.BoardEntity(
                name = Board.FREE.buttonName,
                type = boardId,
                useYn = true
            )
            val savedBoard = boardRepository.save(board)
            // 만약 저장된 board의 id가 boardId와 다르면, boardId를 사용
            val actualBoardId = if (savedBoard.id == boardId) boardId else savedBoard.id!!
            
            val post = PostEntity(
                boardId = actualBoardId,
                userId = userId,
                title = "테스트 게시글",
                content = "테스트 내용",
                contentHtml = "<p>테스트 내용</p>",
                authorNickname = "테스터",
                useYn = true
            )
            val saved = postRepository.save(post)
            return saved.id!!
        } else {
            val post = PostEntity(
                boardId = boardId,
                userId = userId,
                title = "테스트 게시글",
                content = "테스트 내용",
                contentHtml = "<p>테스트 내용</p>",
                authorNickname = "테스터",
                useYn = true
            )
            val saved = postRepository.save(post)
            return saved.id!!
        }
    }

    @Test
    @DisplayName("게시글 수정 - 작성자만 가능")
    fun shouldUpdatePost_whenOwner() {
        val request = mapOf("title" to "수정된 제목")

        // 작성자가 수정하는 경우 - 성공
        mockMvc.perform(
            put("/api/posts/$postId")
                .header("Authorization", ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)

        // 다른 사용자가 수정하려는 경우 - 실패 (403 Forbidden)
        val result = mockMvc.perform(
            put("/api/posts/$postId")
                .header("Authorization", otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andReturn()

        println("=== 게시글 수정 권한 테스트 ===")
        println("상태 코드: ${result.response.status}")
        println("응답 본문: ${result.response.contentAsString}")

        // 401은 인증 문제이므로, 403 또는 400이어야 함 (401은 허용하지 않음)
        org.junit.jupiter.api.Assertions.assertTrue(
            result.response.status in listOf(403, 400),
            "예상: 403 Forbidden 또는 400 Bad Request, 실제: ${result.response.status}, 응답: ${result.response.contentAsString}"
        )
        
        // 응답 본문에 에러 메시지가 포함되어 있는지 확인
        val responseBody = result.response.contentAsString
        org.junit.jupiter.api.Assertions.assertTrue(
            responseBody.contains("작성자") || responseBody.contains("권한") || responseBody.contains("접근"),
            "응답 본문에 에러 메시지가 없습니다: $responseBody"
        )
    }

    @Test
    @DisplayName("게시글 삭제 - 작성자만 가능")
    fun shouldDeletePost_whenOwner() {
        val newPostId = createPost(ownerMember.id!!)

        // 작성자가 삭제하는 경우 - 성공
        mockMvc.perform(
            delete("/api/posts/$newPostId")
                .header("Authorization", ownerToken)
        )
            .andExpect(status().isNoContent)

        val anotherPostId = createPost(ownerMember.id!!)

        // 다른 사용자가 삭제하려는 경우 - 실패 (403 Forbidden)
        val result = mockMvc.perform(
            delete("/api/posts/$anotherPostId")
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
            responseBody.contains("작성자") || responseBody.contains("권한") || responseBody.contains("접근"),
            "응답 본문에 에러 메시지가 없습니다: $responseBody"
        )
    }
}
