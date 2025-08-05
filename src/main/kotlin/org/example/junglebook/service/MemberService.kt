package org.example.junglebook.service

import kr.co.minust.api.exception.DefaultErrorCode
import kr.co.minust.api.exception.GlobalException
import org.example.junglebook.dto.MemberDto
import org.example.junglebook.entity.MemberEntity
import org.example.junglebook.model.Member
import org.example.junglebook.repository.MemberRepository
import org.example.junglebook.util.toBoolean
import org.springframework.security.authentication.InternalAuthenticationServiceException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MemberService(
    private val memberRepository: MemberRepository
): UserDetailsService {
    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(loginId: String): UserDetails =
        Member.from(findActivateMemberByLoginId(loginId))

    fun myInfoByLoginId(loginId: String) = memberRepository.findByLoginId(loginId)

    @Transactional(readOnly = true)
    fun myInfoById(id: Long): MemberDto.MemberDetailResponse {
        val member = findActivateMemberById(id)
        return MemberDto.MemberDetailResponse(
            id = member.id!!,
            loginId = member.loginId,
            name = member.name,
            email = member.email,
            phoneNumber = member.phoneNumber,
            nickname = member.nickname,
            birth = member.birth,
            sex = member.sex,
            ideology = member.ideology,
            profile =  member.profileImage,
            createdAt = member.createdAt
        )
    }
    @Transactional
    fun signUp(request: MemberDto.SignUpRequest) {
        val member = memberRepository.findFirstByEmail(request.email)
        if (member != null) {
            throw GlobalException(DefaultErrorCode.EMAIL_ALREADY_EXIST)
        }
        val memberEntity = request.toMemberEntity()
        memberRepository.save(memberEntity)
    }

    fun findActivateMemberByLoginId(loginId: String): MemberEntity =
        memberRepository.findByLoginId(loginId)?.let { member ->
            if (member.deleteYn.toBoolean()) throw InternalAuthenticationServiceException(DefaultErrorCode.DELETED_MEMBER.description)
            member
        } ?: throw InternalAuthenticationServiceException(DefaultErrorCode.LOGIN_FAILURE.description)

    fun findActivateMemberById(id: Long): MemberEntity =
        memberRepository.findById(id).orElseThrow { IllegalArgumentException() } .let { member ->
            if (member.deleteYn.toBoolean()) throw InternalAuthenticationServiceException(DefaultErrorCode.DELETED_MEMBER.description)
            member
        }

    @Transactional
    fun passwordUpdate(member: MemberEntity, encodedPassword: String) {
        member.password = encodedPassword
        member.updatedAt = LocalDateTime.now()
        memberRepository.save(member)
    }
}