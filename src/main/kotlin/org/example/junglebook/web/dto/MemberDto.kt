package org.example.junglebook.web.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import org.example.junglebook.entity.MemberEntity
import org.example.junglebook.enums.Ideology
import org.example.junglebook.enums.MemberType
import org.example.junglebook.enums.Sex
import org.example.junglebook.enums.SocialProvider
import java.time.LocalDateTime
// 일반 회원가입
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
            memberType = MemberType.REGULAR,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
}

// 회원 정보 수정
data class MemberUpdateRequest(
    val nickname: String?,
    val profile: String?,
    val ideology: Ideology?,
    val phoneNumber: String?
)

// 비밀번호 변경 요청 (일반 회원만)
data class MemberPasswordUpdateRequest(
    val password: String,
    val newPassword1: String,
    val newPassword2: String
) {
    @JsonIgnore
    fun isCorrect(): Boolean = newPassword1 == newPassword2
}



// 회원 상세 정보 응답 (소셜/일반 구분)
data class MemberDetailResponse(
    val id: Long,
    val loginId: String?,
    val name: String,
    val phoneNumber: String?,
    val email: String,
    val birth: String?,
    val nickname: String,
    val sex: Sex?,
    val ideology: Ideology?,
    val profile: String?,
    val memberType: MemberType,
    val socialProvider: SocialProvider?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(member: MemberEntity): MemberDetailResponse {
            return MemberDetailResponse(
                id = member.id!!,
                loginId = member.loginId,
                name = member.name,
                phoneNumber = member.phoneNumber,
                email = member.email,
                birth = member.birth,
                nickname = member.nickname,
                sex = member.sex,
                ideology = member.ideology,
                profile = member.profileImage,
                memberType = member.memberType,
                socialProvider = member.socialProvider,
                createdAt = member.createdAt
            )
        }
    }
}

// 간단한 회원 정보 응답 (목록용)
data class MemberSimpleResponse(
    val id: Long,
    val name: String,
    val email: String,
    val nickname: String,
    val profile: String?,
    val memberType: MemberType,
    val socialProvider: SocialProvider?
) {
    companion object {
        fun from(member: MemberEntity): MemberSimpleResponse {
            return MemberSimpleResponse(
                id = member.id!!,
                name = member.name,
                email = member.email,
                nickname = member.nickname,
                profile = member.profileImage,
                memberType = member.memberType,
                socialProvider = member.socialProvider
            )
        }
    }
}

// 소셜 로그인 사용자 정보 응답
data class SocialMemberResponse(
    val id: Long,
    val name: String,
    val email: String,
    val nickname: String,
    val profile: String?,
    val socialProvider: SocialProvider,
    val isProfileComplete: Boolean, // 추가 정보 입력 필요 여부
    val missingFields: List<String> // 누락된 필수 정보
) {
    companion object {
        fun from(member: MemberEntity): SocialMemberResponse {
            val missingFields = mutableListOf<String>()

            if (member.birth.isNullOrBlank()) missingFields.add("birth")
            if (member.phoneNumber.isNullOrBlank()) missingFields.add("phoneNumber")
            if (member.sex == null) missingFields.add("sex")
            if (member.ideology == null) missingFields.add("ideology")

            return SocialMemberResponse(
                id = member.id!!,
                name = member.name,
                email = member.email,
                nickname = member.nickname,
                profile = member.profileImage,
                socialProvider = member.socialProvider!!,
                isProfileComplete = missingFields.isEmpty(),
                missingFields = missingFields
            )
        }
    }
}

// 로그인 응답 (일반/소셜 공통)
data class LoginResponse(
    val success: Boolean,
    val member: MemberSimpleResponse?,
    val memberType: MemberType?,
    val requiresAdditionalInfo: Boolean = false, // 소셜 로그인 시 추가 정보 필요 여부
    val message: String
) {
    companion object {
        fun success(member: MemberEntity): LoginResponse {
            val isSocial = member.memberType == MemberType.SOCIAL
            val requiresInfo = isSocial && (
                    member.birth.isNullOrBlank() ||
                            member.phoneNumber.isNullOrBlank() ||
                            member.sex == null ||
                            member.ideology == null
                    )

            return LoginResponse(
                success = true,
                member = MemberSimpleResponse.from(member),
                memberType = member.memberType,
                requiresAdditionalInfo = requiresInfo,
                message = if (requiresInfo) "추가 정보 입력이 필요합니다." else "로그인 성공"
            )
        }

        fun failure(message: String): LoginResponse {
            return LoginResponse(
                success = false,
                member = null,
                memberType = null,
                message = message
            )
        }
    }
}

// 중복 체크 응답 DTO
data class DuplicateCheckResponse(
    val field: String,        // 체크한 필드명 (loginId, email, nickname)
    val value: String,        // 체크한 값
    val isDuplicate: Boolean, // 중복 여부
    val message: String       // 사용자 메시지
)