package org.example.junglebook.service

import mu.KotlinLogging
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.web.dto.NaverUserInfoResponse
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
class NaverOAuthService(
    private val restTemplate: RestTemplate
) {
    companion object {
        private const val NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me"
    }

    fun getUserInfo(accessToken: String): SocialUserInfo {
        try {
            val headers = HttpHeaders().apply {
                set("Authorization", "Bearer $accessToken")
                contentType = MediaType.APPLICATION_JSON
            }
            val entity = HttpEntity<Any>(headers)

            val response = restTemplate.exchange(
                NAVER_USER_INFO_URL,
                HttpMethod.GET,
                entity,
                NaverUserInfoResponse::class.java
            )

            val naverUserInfo = response.body
            requireNotNull(naverUserInfo) { "Naver user info response is null" }

            if (naverUserInfo.resultcode != "00") {
                logger.error { "네이버 API 오류: resultcode=${naverUserInfo.resultcode}, message=${naverUserInfo.message}" }
                throw GlobalException(DefaultErrorCode.INVALID_SOCIAL_TOKEN)
            }

            val userInfo = naverUserInfo.response
            requireNotNull(userInfo) { "Naver user info is null" }

            val userId = userInfo.id
            val email = userInfo.email ?: ""
            val name = userInfo.name ?: userInfo.nickname ?: "네이버사용자"
            val nickname = userInfo.nickname ?: name
            val profileImage = userInfo.profileImage

            logger.info { "네이버 사용자 정보 조회 성공: userId=$userId, email=$email, nickname=$nickname" }

            return SocialUserInfo(
                id = userId,
                name = name,
                email = email,
                nickname = nickname,
                profileImage = profileImage
            )
        } catch (e: HttpClientErrorException) {
            logger.error(e) { "네이버 API 호출 실패: status=${e.statusCode}, body=${e.responseBodyAsString}" }
            when (e.statusCode) {
                HttpStatus.UNAUTHORIZED -> throw GlobalException(DefaultErrorCode.INVALID_SOCIAL_TOKEN)
                HttpStatus.BAD_REQUEST -> throw GlobalException(DefaultErrorCode.INVALID_SOCIAL_TOKEN)
                else -> throw GlobalException(DefaultErrorCode.EXTERNAL_API_ERROR)
            }
        } catch (e: GlobalException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "네이버 사용자 정보 조회 중 예외 발생" }
            throw GlobalException(DefaultErrorCode.EXTERNAL_API_ERROR)
        }
    }
}

