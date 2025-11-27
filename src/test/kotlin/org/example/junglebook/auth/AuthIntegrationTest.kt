package org.example.junglebook.auth

import org.example.junglebook.service.JwtService
import org.example.junglebook.model.Member
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private lateinit var jwtService: JwtService

    @Test
    fun `JWT 서비스 빈 로딩 테스트`() {
        // JWT 서비스가 정상적으로 로딩되는지 확인
        assertNotNull(jwtService)
    }

    @Test
    fun `JWT 토큰 생성 테스트`() {
        // Given
        val memberEntity = org.example.junglebook.entity.MemberEntity(
            name = "테스트사용자",
            birth = "19900101",
            phoneNumber = "01012345678",
            email = "test@example.com",
            sex = org.example.junglebook.enums.Sex.M,
            ideology = org.example.junglebook.enums.Ideology.M,
            loginId = "testuser",
            nickname = "테스트닉네임",
            password = "password",
            profileImage = "",
            deleteYn = 0,
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = java.time.LocalDateTime.now(),
            socialProvider = null,
            socialProviderId = null,
            memberType = org.example.junglebook.enums.MemberType.REGULAR
        )
        val member = Member.from(memberEntity)

        // When
        val accessToken = jwtService.generateToken(member, true)
        val refreshToken = jwtService.generateToken(member, false)

        // Then
        assertNotNull(accessToken)
        assertNotNull(refreshToken)
        assertTrue(accessToken.isNotEmpty())
        assertTrue(refreshToken.isNotEmpty())
    }
}
