package org.example.junglebook.dto

data class TokenDto(
    val memberId: Long? = null,
    val nickname: String? = null,
    val accessToken: String,
    val refreshToken: String? = null
)