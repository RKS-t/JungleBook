package org.example.junglebook.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import org.example.junglebook.constant.JBConstants
import org.example.junglebook.entity.MemberEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import java.time.ZoneId

class Member private constructor(
    val loginId: String,
    val pwd: String,
    val issued: Long,
    var expired: Long
): UserDetails {
    companion object {
        fun from(memberEntity: MemberEntity, expireDays: Long? = JBConstants.DEFAULT_EXPIRE_DAYS) = LocalDateTime.now().let { now ->
            Member(
                memberEntity.loginId!!,
                memberEntity.password!!,
                now.atZone(ZoneId.of(JBConstants.ZONE_ID_SEOUL)).toInstant().toEpochMilli(),
                now.plusDays(expireDays!!).atZone(ZoneId.of(JBConstants.ZONE_ID_SEOUL)).toInstant().toEpochMilli()
            )
        }
    }

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = mutableListOf()

    override fun getPassword() = pwd

    override fun getUsername() = loginId

    override fun isEnabled() = true

    override fun isCredentialsNonExpired() = true

    override fun isAccountNonExpired() = true

    override fun isAccountNonLocked() = true

    fun setExpired(expireDays: Long): Member {
        this.expired = LocalDateTime.now()
            .plusDays(expireDays)
            .atZone(ZoneId.of(JBConstants.ZONE_ID_SEOUL))
            .toInstant()
            .toEpochMilli()

        return this
    }

    fun toJwtPayload(isAccessToken: Boolean) = JwtPayload(this.loginId, isAccessToken)
}

@Serializable
data class JwtPayload(
    val loginId: String,
    @SerialName("isAccessToken")
    val isAccessToken: Boolean
)