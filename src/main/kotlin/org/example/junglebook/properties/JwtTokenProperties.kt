package org.example.junglebook.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(value = "jwt.expire-days")
data class JwtTokenProperties(
    val accessToken: Long,
    val refreshToken: Long
)
