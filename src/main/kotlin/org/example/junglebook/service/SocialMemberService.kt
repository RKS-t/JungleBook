package org.example.junglebook.service

import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.entity.MemberEntity
import org.example.junglebook.enums.Ideology
import org.example.junglebook.enums.MemberType
import org.example.junglebook.enums.Sex
import org.example.junglebook.enums.SocialProvider
import org.example.junglebook.repository.MemberRepository
import org.example.junglebook.web.dto.LoginResponse
import org.example.junglebook.web.dto.SocialMemberResponse
import org.example.junglebook.web.dto.SocialSignUpCompleteRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class SocialMemberService(
    private val memberRepository: MemberRepository,
    private val memberService: MemberService
) {

    @Transactional(readOnly = true)
    fun findBySocialLogin(provider: SocialProvider, providerId: String): MemberEntity? {
        return memberRepository.findBySocialProviderAndSocialProviderId(provider, providerId)
            ?.takeIf { it.deleteYn == 0 }
    }

    fun createOrLinkSocialMember(
        name: String,
        email: String,
        profileImage: String?,
        provider: SocialProvider,
        providerId: String
    ): MemberEntity {
        val existingMember = memberRepository.findByEmail(email)?.takeIf { it.deleteYn == 0 }
        existingMember?.let { member ->
            return linkSocialAccountInternal(member, provider, providerId, profileImage)
        }

        val nickname = generateUniqueNickname(name)
        val loginId = "social_${provider.name.lowercase()}_${providerId}"

        val newMember = MemberEntity(
            name = name,
            email = email,
            nickname = nickname,
            profileImage = profileImage ?: "",
            socialProvider = provider,
            socialProviderId = providerId,
            memberType = MemberType.SOCIAL,
            loginId = loginId,
            password = "",
            birth = "",
            phoneNumber = "",
            sex = Sex.M,
            ideology = Ideology.N,
            deleteYn = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        return memberRepository.save(newMember)
    }

    fun updateSocialMember(
        member: MemberEntity,
        name: String,
        email: String,
        profileImage: String?
    ): MemberEntity {
        member.name = name
        member.email = email
        member.profileImage = profileImage ?: member.profileImage
        member.updatedAt = LocalDateTime.now()

        return memberRepository.save(member)
    }

    fun completeSocialProfile(loginId: String, request: SocialSignUpCompleteRequest): MemberEntity {
        val member = memberService.findActivateMemberByLoginId(loginId)

        if (member.memberType != MemberType.SOCIAL) {
            throw GlobalException(DefaultErrorCode.SOCIAL_ACCOUNT_NOT_LINKED)
        }

        request.birth?.let { member.birth = it }
        request.phoneNumber?.let { member.phoneNumber = it }
        request.sex?.let { member.sex = it }
        request.ideology?.let { member.ideology = it }
        member.updatedAt = LocalDateTime.now()

        return memberRepository.save(member)
    }

    fun linkSocialAccount(
        memberId: Long,
        provider: SocialProvider,
        providerId: String,
        profileImage: String?
    ): MemberEntity {
        val member = memberService.findActivateMemberById(memberId)
        return linkSocialAccountInternal(member, provider, providerId, profileImage)
    }

    fun unlinkSocialAccount(memberId: Long): MemberEntity {
        val member = memberService.findActivateMemberById(memberId)

        if (member.socialProvider == null) {
            throw GlobalException(DefaultErrorCode.SOCIAL_ACCOUNT_NOT_LINKED)
        }

        if (member.memberType == MemberType.SOCIAL && member.loginId.startsWith("social_")) {
            throw GlobalException(DefaultErrorCode.SOCIAL_ONLY_MEMBER_UNLINK_DENIED)
        }

        member.socialProvider = null
        member.socialProviderId = null
        member.updatedAt = LocalDateTime.now()

        return memberRepository.save(member)
    }

    @Transactional(readOnly = true)
    fun isSocialProfileComplete(member: MemberEntity): Boolean {
        return member.memberType != MemberType.SOCIAL || (
                member.birth.isNotBlank() &&
                        member.phoneNumber.isNotBlank() &&
                        member.sex != null &&
                        member.ideology != null &&
                        member.ideology != Ideology.N
                )
    }

    @Transactional(readOnly = true)
    fun getMissingProfileFields(member: MemberEntity): List<String> {
        if (member.memberType != MemberType.SOCIAL) return emptyList()

        val missingFields = mutableListOf<String>()
        if (member.birth.isBlank()) missingFields.add("birth")
        if (member.phoneNumber.isBlank()) missingFields.add("phoneNumber")
        if (member.sex == null) missingFields.add("sex")
        if (member.ideology == null || member.ideology == Ideology.N) missingFields.add("ideology")

        return missingFields
    }

    @Transactional(readOnly = true)
    fun createSocialMemberResponse(member: MemberEntity): SocialMemberResponse {
        return SocialMemberResponse.from(member)
    }

    @Transactional(readOnly = true)
    fun findExistingMemberByEmail(email: String): MemberEntity? {
        return memberRepository.findByEmail(email)?.takeIf { it.deleteYn == 0 }
    }

    @Transactional(readOnly = true)
    fun isSocialAccountAlreadyLinked(provider: SocialProvider, providerId: String, excludeMemberId: Long? = null): Boolean {
        val existingMember = findBySocialLogin(provider, providerId)
        return existingMember != null && existingMember.id != excludeMemberId
    }

    private fun linkSocialAccountInternal(
        member: MemberEntity,
        provider: SocialProvider,
        providerId: String,
        profileImage: String?
    ): MemberEntity {
        if (member.socialProvider != null) {
            throw GlobalException(DefaultErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED)
        }

        if (isSocialAccountAlreadyLinked(provider, providerId, member.id)) {
            throw GlobalException(DefaultErrorCode.SOCIAL_ACCOUNT_LINKED_TO_OTHER)
        }

        member.socialProvider = provider
        member.socialProviderId = providerId
        profileImage?.let { member.profileImage = it }
        member.updatedAt = LocalDateTime.now()

        return memberRepository.save(member)
    }

    private fun generateUniqueNickname(baseName: String): String {
        var nickname = baseName.replace(" ", "").take(10)
        var counter = 1

        if (nickname.isBlank()) {
            nickname = "user"
        }

        var candidateNickname = nickname
        while (memberService.findByNickname(candidateNickname) != null) {
            candidateNickname = "${nickname}_${counter}"
            counter++

            if (counter > 9999) {
                candidateNickname = "user_${System.currentTimeMillis()}"
                break
            }
        }

        return candidateNickname
    }

    @Transactional(readOnly = true)
    fun createLoginResponse(member: MemberEntity): LoginResponse {
        return LoginResponse.success(member)
    }

    @Transactional(readOnly = true)
    fun requiresAdditionalInfo(member: MemberEntity): Boolean {
        return member.memberType == MemberType.SOCIAL && !isSocialProfileComplete(member)
    }
}