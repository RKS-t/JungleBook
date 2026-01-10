package org.example.junglebook.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.web.dto.NaverUserInfo
import org.example.junglebook.web.dto.NaverUserInfoResponse
import org.example.junglebook.web.dto.SocialUserInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@ExtendWith(MockitoExtension::class)
class NaverOAuthServiceTest {

    @Mock
    private lateinit var restTemplate: RestTemplate

    @InjectMocks
    private lateinit var naverOAuthService: NaverOAuthService

    private val accessToken = "test_access_token"

    @Test
    fun `getUserInfo - 성공 케이스`() {
        val naverResponse = NaverUserInfoResponse(
            resultcode = "00",
            message = "success",
            response = NaverUserInfo(
                id = "naver_123456",
                email = "test@example.com",
                name = "테스트유저",
                nickname = "테스트닉네임",
                profileImage = "https://example.com/profile.jpg"
            )
        )

        val responseEntity = ResponseEntity.ok(naverResponse)
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(NaverUserInfoResponse::class.java))).thenReturn(responseEntity)

        val result = naverOAuthService.getUserInfo(accessToken)

        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo("naver_123456")
        assertThat(result.email).isEqualTo("test@example.com")
        assertThat(result.name).isEqualTo("테스트유저")
        assertThat(result.nickname).isEqualTo("테스트닉네임")
        assertThat(result.profileImage).isEqualTo("https://example.com/profile.jpg")
    }

    @Test
    fun `getUserInfo - 닉네임이 없을 때 name 사용`() {
        val naverResponse = NaverUserInfoResponse(
            resultcode = "00",
            message = "success",
            response = NaverUserInfo(
                id = "naver_123456",
                email = "test@example.com",
                name = "테스트유저",
                nickname = null,
                profileImage = null
            )
        )

        val responseEntity = ResponseEntity.ok(naverResponse)
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(NaverUserInfoResponse::class.java))).thenReturn(responseEntity)

        val result = naverOAuthService.getUserInfo(accessToken)

        assertThat(result.nickname).isEqualTo("테스트유저")
    }

    @Test
    fun `getUserInfo - name과 nickname이 없을 때 기본값 사용`() {
        val naverResponse = NaverUserInfoResponse(
            resultcode = "00",
            message = "success",
            response = NaverUserInfo(
                id = "naver_123456",
                email = "test@example.com",
                name = null,
                nickname = null,
                profileImage = null
            )
        )

        val responseEntity = ResponseEntity.ok(naverResponse)
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(NaverUserInfoResponse::class.java))).thenReturn(responseEntity)

        val result = naverOAuthService.getUserInfo(accessToken)

        assertThat(result.name).isEqualTo("네이버사용자")
        assertThat(result.nickname).isEqualTo("네이버사용자")
    }

    @Test
    fun `getUserInfo - 이메일이 없을 때 빈 문자열 반환`() {
        val naverResponse = NaverUserInfoResponse(
            resultcode = "00",
            message = "success",
            response = NaverUserInfo(
                id = "naver_123456",
                email = null,
                name = "테스트유저",
                nickname = "테스트닉네임",
                profileImage = null
            )
        )

        val responseEntity = ResponseEntity.ok(naverResponse)
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(NaverUserInfoResponse::class.java))).thenReturn(responseEntity)

        val result = naverOAuthService.getUserInfo(accessToken)

        assertThat(result.email).isEmpty()
    }

    @Test
    fun `getUserInfo - resultcode가 00이 아닐 때 예외 발생`() {
        val naverResponse = NaverUserInfoResponse(
            resultcode = "024",
            message = "Authentication failed",
            response = NaverUserInfo(
                id = "naver_123456",
                email = null,
                name = null,
                nickname = null,
                profileImage = null
            )
        )

        val responseEntity = ResponseEntity.ok(naverResponse)
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(NaverUserInfoResponse::class.java))).thenReturn(responseEntity)

        assertThatThrownBy { naverOAuthService.getUserInfo(accessToken) }
            .isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.INVALID_SOCIAL_TOKEN
            }
    }

    @Test
    fun `getUserInfo - 401 Unauthorized 예외 처리`() {
        val exception = HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized")
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(NaverUserInfoResponse::class.java))).thenThrow(exception)

        assertThatThrownBy { naverOAuthService.getUserInfo(accessToken) }
            .isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.INVALID_SOCIAL_TOKEN
            }
    }

    @Test
    fun `getUserInfo - 400 Bad Request 예외 처리`() {
        val exception = HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request")
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(NaverUserInfoResponse::class.java))).thenThrow(exception)

        assertThatThrownBy { naverOAuthService.getUserInfo(accessToken) }
            .isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.INVALID_SOCIAL_TOKEN
            }
    }

    @Test
    fun `getUserInfo - 기타 HTTP 예외 처리`() {
        val exception = HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error")
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(NaverUserInfoResponse::class.java))).thenThrow(exception)

        assertThatThrownBy { naverOAuthService.getUserInfo(accessToken) }
            .isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.EXTERNAL_API_ERROR
            }
    }

    @Test
    fun `getUserInfo - 일반 예외 처리`() {
        val exception = RuntimeException("Network error")
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(NaverUserInfoResponse::class.java))).thenThrow(exception)

        assertThatThrownBy { naverOAuthService.getUserInfo(accessToken) }
            .isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.EXTERNAL_API_ERROR
            }
    }
}

