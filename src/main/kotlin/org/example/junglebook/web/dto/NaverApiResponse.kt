package org.example.junglebook.web.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class NaverUserInfoResponse(
    val resultcode: String,
    val message: String,
    val response: NaverUserInfo?
)

data class NaverUserInfo(
    val id: String,
    val email: String?,
    val name: String?,
    val nickname: String?,
    @JsonProperty("profile_image")
    val profileImage: String?
)

data class NaverErrorResponse(
    val resultcode: String,
    val message: String
)

