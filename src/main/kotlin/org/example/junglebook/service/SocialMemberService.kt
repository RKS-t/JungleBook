package org.example.junglebook.service


import kr.co.minust.api.exception.DefaultErrorCode
import kr.co.minust.api.exception.GlobalException
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

    /**
     * 소셜 로그인 정보로 회원 조회
     */
    @Transactional(readOnly = true)
    fun findBySocialLogin(provider: SocialProvider, providerId: String): MemberEntity? {
        return memberRepository.findBySocialProviderAndSocialProviderId(provider, providerId)
            ?.takeIf { it.deleteYn == 0 }
    }

    /**
     * 새로운 소셜 회원 생성 또는 기존 회원과 연동
     */
    fun createOrLinkSocialMember(
        name: String,
        email: String,
        profileImage: String?,
        provider: SocialProvider,
        providerId: String
    ): MemberEntity {
        // 이메일로 기존 일반 회원 확인 (소셜 로그인 연동)
        val existingMember = memberRepository.findByEmail(email)?.takeIf { it.deleteYn == 0 }
        existingMember?.let { member ->
            return linkSocialAccountInternal(member, provider, providerId, profileImage)
        }

        // 새로운 소셜 회원 생성
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
            password = "", // 소셜 로그인은 비밀번호 불필요
            birth = "", // 추후 입력
            phoneNumber = "", // 추후 입력
            sex = Sex.M, // 기본값 설정 (추후 변경 가능)
            ideology = Ideology.N, // 기본값 설정 (추후 변경 가능)
            deleteYn = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        return memberRepository.save(newMember)
    }

    /**
     * 기존 소셜 회원 정보 업데이트
     */
    fun updateSocialMember(
        member: MemberEntity,
        name: String,
        email: String,
        profileImage: String?
    ): MemberEntity {
        // 직접 필드 수정 (JPA Entity이므로 copy() 사용하지 않음)
        member.name = name
        member.email = email
        member.profileImage = profileImage ?: member.profileImage
        member.updatedAt = LocalDateTime.now()

        return memberRepository.save(member)
    }

    /**
     * 소셜 로그인 후 추가 정보 입력 완료
     */
    fun completeSocialProfile(loginId: String, request: SocialSignUpCompleteRequest): MemberEntity {
        val member = memberService.findActivateMemberByLoginId(loginId)

        if (member.memberType != MemberType.SOCIAL) {
            throw GlobalException(DefaultErrorCode.SOCIAL_ACCOUNT_NOT_LINKED)
        }

        // 직접 필드 수정
        request.birth?.let { member.birth = it }
        request.phoneNumber?.let { member.phoneNumber = it }
        request.sex?.let { member.sex = it }
        request.ideology?.let { member.ideology = it }
        member.updatedAt = LocalDateTime.now()

        return memberRepository.save(member)
    }

    /**
     * 기존 회원에 소셜 계정 연동
     */
    fun linkSocialAccount(
        memberId: Long,
        provider: SocialProvider,
        providerId: String,
        profileImage: String?
    ): MemberEntity {
        val member = memberService.findActivateMemberById(memberId)
        return linkSocialAccountInternal(member, provider, providerId, profileImage)
    }

    /**
     * 소셜 계정 연동 해제
     */
    fun unlinkSocialAccount(memberId: Long): MemberEntity {
        val member = memberService.findActivateMemberById(memberId)

        if (member.socialProvider == null) {
            throw GlobalException(DefaultErrorCode.SOCIAL_ACCOUNT_NOT_LINKED)
        }

        // 소셜 전용 회원이면 연동 해제 불가 (loginId가 social_로 시작하는 경우)
        if (member.memberType == MemberType.SOCIAL && member.loginId.startsWith("social_")) {
            throw GlobalException(DefaultErrorCode.SOCIAL_ONLY_MEMBER_UNLINK_DENIED)
        }

        // 직접 필드 수정
        member.socialProvider = null
        member.socialProviderId = null
        member.updatedAt = LocalDateTime.now()

        return memberRepository.save(member)
    }

    /**
     * 소셜 회원 프로필 완성도 체크
     */
    @Transactional(readOnly = true)
    fun isSocialProfileComplete(member: MemberEntity): Boolean {
        return member.memberType != MemberType.SOCIAL || (
                member.birth.isNotBlank() &&
                        member.phoneNumber.isNotBlank() &&
                        member.sex != null &&
                        member.ideology != null &&
                        member.ideology != Ideology.N // N(없음)이 아닌 경우만 완성으로 간주
                )
    }

    /**
     * 소셜 회원의 누락된 필수 정보 반환
     */
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

    /**
     * 소셜 회원 응답 DTO 생성
     */
    @Transactional(readOnly = true)
    fun createSocialMemberResponse(member: MemberEntity): SocialMemberResponse {
        return SocialMemberResponse.from(member)
    }

    /**
     * 이메일로 기존 회원 확인 (소셜 연동용)
     */
    @Transactional(readOnly = true)
    fun findExistingMemberByEmail(email: String): MemberEntity? {
        return memberRepository.findByEmail(email)?.takeIf { it.deleteYn == 0 }
    }

    /**
     * 소셜 계정이 이미 다른 회원에게 연결되어 있는지 확인
     */
    @Transactional(readOnly = true)
    fun isSocialAccountAlreadyLinked(provider: SocialProvider, providerId: String, excludeMemberId: Long? = null): Boolean {
        val existingMember = findBySocialLogin(provider, providerId)
        return existingMember != null && existingMember.id != excludeMemberId
    }

    /**
     * 소셜 계정 연동 내부 로직
     */
    private fun linkSocialAccountInternal(
        member: MemberEntity,
        provider: SocialProvider,
        providerId: String,
        profileImage: String?
    ): MemberEntity {
        // 이미 다른 소셜 계정이 연결되어 있는지 확인
        if (member.socialProvider != null) {
            throw GlobalException(DefaultErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED)
        }

        // 해당 소셜 계정이 다른 사용자에게 연결되어 있는지 확인
        if (isSocialAccountAlreadyLinked(provider, providerId, member.id)) {
            throw GlobalException(DefaultErrorCode.SOCIAL_ACCOUNT_LINKED_TO_OTHER)
        }

        // 직접 필드 수정
        member.socialProvider = provider
        member.socialProviderId = providerId
        profileImage?.let { member.profileImage = it }
        member.updatedAt = LocalDateTime.now()

        return memberRepository.save(member)
    }

    /**
     * 유니크한 닉네임 생성
     */
    private fun generateUniqueNickname(baseName: String): String {
        var nickname = baseName.replace(" ", "").take(10) // 공백 제거 및 길이 제한
        var counter = 1

        // 기본 닉네임이 비어있거나 특수문자만 있는 경우 기본값 설정
        if (nickname.isBlank()) {
            nickname = "user"
        }

        var candidateNickname = nickname
        while (memberService.findByNickname(candidateNickname) != null) {
            candidateNickname = "${nickname}_${counter}"
            counter++

            // 무한루프 방지를 위한 카운터 제한
            if (counter > 9999) {
                candidateNickname = "user_${System.currentTimeMillis()}"
                break
            }
        }

        return candidateNickname
    }

    /**
     * 소셜 로그인 결과 DTO 생성
     */
    @Transactional(readOnly = true)
    fun createLoginResponse(member: MemberEntity): LoginResponse {
        return LoginResponse.success(member)
    }

    /**
     * 회원 타입별 추가 정보 필요 여부 확인
     */
    @Transactional(readOnly = true)
    fun requiresAdditionalInfo(member: MemberEntity): Boolean {
        return member.memberType == MemberType.SOCIAL && !isSocialProfileComplete(member)
    }
}