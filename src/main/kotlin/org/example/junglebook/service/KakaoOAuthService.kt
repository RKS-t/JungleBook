package org.example.junglebook.service

import mu.KotlinLogging
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.web.dto.KakaoUserInfoResponse
import org.example.junglebook.web.dto.SocialUserInfo
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

private val logger = KotlinLogging.logger {}

@Service
class KakaoOAuthService(
    private val restTemplate: RestTemplate
) {
    companion object {
        private const val KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me"
    }

    fun getUserInfo(accessToken: String): SocialUserInfo {
        try {
            val headers = HttpHeaders().apply {
                set("Authorization", "Bearer $accessToken")
                contentType = MediaType.APPLICATION_JSON
            }
            val entity = HttpEntity<Any>(headers)

            val response = restTemplate.exchange(
                KAKAO_USER_INFO_URL,
                HttpMethod.GET,
                entity,
                KakaoUserInfoResponse::class.java
            )

            val kakaoUserInfo = response.body
            requireNotNull(kakaoUserInfo) { "Kakao user info response is null" }

            val kakaoAccount = kakaoUserInfo.kakaoAccount
            val profile = kakaoAccount?.profile

            val userId = kakaoUserInfo.id.toString()
            val email = kakaoAccount?.email ?: ""
            val nickname = profile?.nickname ?: "카카오사용자"
            val profileImage = profile?.profileImageUrl ?: profile?.thumbnailImageUrl

            logger.info { "카카오 사용자 정보 조회 성공: userId=$userId, email=$email, nickname=$nickname" }

            return SocialUserInfo(
                id = userId,
                name = nickname,
                email = email,
                nickname = nickname,
                profileImage = profileImage
            )
        } catch (e: HttpClientErrorException) {
            logger.error(e) { "카카오 API 호출 실패: status=${e.statusCode}, body=${e.responseBodyAsString}" }
            when (e.statusCode) {
                HttpStatus.UNAUTHORIZED -> throw GlobalException(DefaultErrorCode.INVALID_SOCIAL_TOKEN)
                HttpStatus.BAD_REQUEST -> throw GlobalException(DefaultErrorCode.INVALID_SOCIAL_TOKEN)
                else -> throw GlobalException(DefaultErrorCode.EXTERNAL_API_ERROR)
            }
        } catch (e: Exception) {
            logger.error(e) { "카카오 사용자 정보 조회 중 예외 발생" }
            throw GlobalException(DefaultErrorCode.EXTERNAL_API_ERROR)
        }
    }
}

