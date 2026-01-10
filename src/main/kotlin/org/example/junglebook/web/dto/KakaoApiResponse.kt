package org.example.junglebook.web.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoUserInfoResponse(
    val id: Long,
    @JsonProperty("kakao_account")
    val kakaoAccount: KakaoAccount?
)

data class KakaoAccount(
    val email: String?,
    val profile: KakaoProfile?
)

data class KakaoProfile(
    val nickname: String?,
    @JsonProperty("profile_image_url")
    val profileImageUrl: String?,
    @JsonProperty("thumbnail_image_url")
    val thumbnailImageUrl: String?
)

data class KakaoErrorResponse(
    val msg: String,
    val code: Int
)

