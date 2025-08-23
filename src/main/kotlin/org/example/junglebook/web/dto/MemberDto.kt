package org.example.junglebook.web.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import org.example.junglebook.entity.MemberEntity
import org.example.junglebook.enums.Ideology
import org.example.junglebook.enums.Sex
import java.time.LocalDateTime

class MemberDto {
    data class SignUpRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val phoneNumber: String,
        val email: String,
        val birth: String,
        val nickname: String,
        val sex: Sex,
        val ideology: Ideology,
        val profile: String
    ) {
        @JsonIgnore
        lateinit var encodedPassword: String

        fun toMemberEntity(): MemberEntity =
            MemberEntity(
                loginId = loginId,
                password = encodedPassword,
                name = name,
                birth = birth,
                sex = sex,
                phoneNumber = phoneNumber,
                ideology = ideology,
                email = email,
                profileImage = profile,
                nickname = nickname,
                deleteYn = 0,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
    }

    data class MemberUpdateRequest(
        val nickname: String,
        val profile: String,
        val ideology: Ideology
    )

    data class MemberPasswordUpdateRequest(
        val password: String,
        val newPassword1: String,
        val newPassword2: String
    ) {
        @JsonIgnore
        fun isCorrect(): Boolean = newPassword1 == newPassword2
    }

    data class MemberDetailResponse(
        val id: Long,
        val loginId: String,
        val name: String,
        val phoneNumber: String,
        val email: String,
        val birth: String,
        val nickname: String,
        val sex: Sex,
        val ideology: Ideology,
        val profile: String,
        val createdAt: LocalDateTime
    )
}