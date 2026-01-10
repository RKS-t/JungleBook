package org.example.junglebook.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.web.dto.KakaoAccount
import org.example.junglebook.web.dto.KakaoProfile
import org.example.junglebook.web.dto.KakaoUserInfoResponse
import org.example.junglebook.web.dto.SocialUserInfo
import org.junit.jupiter.api.BeforeEach
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
class KakaoOAuthServiceTest {

    @Mock
    private lateinit var restTemplate: RestTemplate

    @InjectMocks
    private lateinit var kakaoOAuthService: KakaoOAuthService

    private val accessToken = "test_access_token"

    @Test
    fun `getUserInfo - 성공 케이스`() {
        val kakaoResponse = KakaoUserInfoResponse(
            id = 123456789L,
            kakaoAccount = KakaoAccount(
                email = "test@example.com",
                profile = KakaoProfile(
                    nickname = "테스트유저",
                    profileImageUrl = "https://example.com/profile.jpg",
                    thumbnailImageUrl = "https://example.com/thumbnail.jpg"
                )
            )
        )

        val responseEntity = ResponseEntity.ok(kakaoResponse)
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(KakaoUserInfoResponse::class.java))).thenReturn(responseEntity)

        val result = kakaoOAuthService.getUserInfo(accessToken)

        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo("123456789")
        assertThat(result.email).isEqualTo("test@example.com")
        assertThat(result.nickname).isEqualTo("테스트유저")
        assertThat(result.profileImage).isEqualTo("https://example.com/profile.jpg")
    }

    @Test
    fun `getUserInfo - 프로필 이미지가 없을 때 썸네일 사용`() {
        val kakaoResponse = KakaoUserInfoResponse(
            id = 123456789L,
            kakaoAccount = KakaoAccount(
                email = "test@example.com",
                profile = KakaoProfile(
                    nickname = "테스트유저",
                    profileImageUrl = null,
                    thumbnailImageUrl = "https://example.com/thumbnail.jpg"
                )
            )
        )

        val responseEntity = ResponseEntity.ok(kakaoResponse)
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(KakaoUserInfoResponse::class.java))).thenReturn(responseEntity)

        val result = kakaoOAuthService.getUserInfo(accessToken)

        assertThat(result.profileImage).isEqualTo("https://example.com/thumbnail.jpg")
    }

    @Test
    fun `getUserInfo - 이메일이 없을 때 빈 문자열 반환`() {
        val kakaoResponse = KakaoUserInfoResponse(
            id = 123456789L,
            kakaoAccount = KakaoAccount(
                email = null,
                profile = KakaoProfile(
                    nickname = "테스트유저",
                    profileImageUrl = null,
                    thumbnailImageUrl = null
                )
            )
        )

        val responseEntity = ResponseEntity.ok(kakaoResponse)
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(KakaoUserInfoResponse::class.java))).thenReturn(responseEntity)

        val result = kakaoOAuthService.getUserInfo(accessToken)

        assertThat(result.email).isEmpty()
    }

    @Test
    fun `getUserInfo - 닉네임이 없을 때 기본값 사용`() {
        val kakaoResponse = KakaoUserInfoResponse(
            id = 123456789L,
            kakaoAccount = KakaoAccount(
                email = "test@example.com",
                profile = KakaoProfile(
                    nickname = null,
                    profileImageUrl = null,
                    thumbnailImageUrl = null
                )
            )
        )

        val responseEntity = ResponseEntity.ok(kakaoResponse)
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(KakaoUserInfoResponse::class.java))).thenReturn(responseEntity)

        val result = kakaoOAuthService.getUserInfo(accessToken)

        assertThat(result.nickname).isEqualTo("카카오사용자")
    }

    @Test
    fun `getUserInfo - 401 Unauthorized 예외 처리`() {
        val exception = HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized")
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(KakaoUserInfoResponse::class.java))).thenThrow(exception)

        assertThatThrownBy { kakaoOAuthService.getUserInfo(accessToken) }
            .isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.INVALID_SOCIAL_TOKEN
            }
    }

    @Test
    fun `getUserInfo - 400 Bad Request 예외 처리`() {
        val exception = HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request")
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(KakaoUserInfoResponse::class.java))).thenThrow(exception)

        assertThatThrownBy { kakaoOAuthService.getUserInfo(accessToken) }
            .isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.INVALID_SOCIAL_TOKEN
            }
    }

    @Test
    fun `getUserInfo - 기타 HTTP 예외 처리`() {
        val exception = HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error")
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(KakaoUserInfoResponse::class.java))).thenThrow(exception)

        assertThatThrownBy { kakaoOAuthService.getUserInfo(accessToken) }
            .isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.EXTERNAL_API_ERROR
            }
    }

    @Test
    fun `getUserInfo - 일반 예외 처리`() {
        val exception = RuntimeException("Network error")
        whenever(restTemplate.exchange(anyString(), any(), any(), eq(KakaoUserInfoResponse::class.java))).thenThrow(exception)

        assertThatThrownBy { kakaoOAuthService.getUserInfo(accessToken) }
            .isInstanceOf(GlobalException::class.java)
            .matches { exception ->
                val globalException = exception as GlobalException
                globalException.code == DefaultErrorCode.EXTERNAL_API_ERROR
            }
    }
}

