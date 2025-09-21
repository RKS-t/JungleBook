package org.example.junglebook.web.dto

import org.example.junglebook.enums.Ideology
import org.example.junglebook.enums.Sex
import org.example.junglebook.enums.SocialProvider

// 소셜 회원가입 요청 DTO
data class SocialSignUpRequest(
    val provider: SocialProvider,
    val accessToken: String
)

// 소셜 회원가입 추가 정보 입력
data class SocialSignUpCompleteRequest(
    val birth: String?,
    val phoneNumber: String?,
    val sex: Sex?,
    val ideology: Ideology?
)


// 소셜 계정 연동 요청
data class SocialLinkRequest(
    val provider: SocialProvider,
    val providerId: String
)


// 소셜 로그인 요청 DTO
data class SocialLoginRequest(
    val provider: SocialProvider,
    val accessToken: String
)

// 소셜 로그인 응답 DTO
data class SocialLoginTokenDto(
    val memberId: Long,
    val nickname: String,
    val accessToken: String,
    val refreshToken: String,
    val loginResponse: LoginResponse // 기존 LoginResponse 활용
)

// 소셜 사용자 정보
data class SocialUserInfo(
    val id: String,
    val name: String,
    val email: String,
    val nickname: String?,
    val profileImage: String?
)