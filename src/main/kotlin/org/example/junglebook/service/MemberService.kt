package org.example.junglebook.service

import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.entity.MemberCampHistoryEntity
import org.example.junglebook.entity.MemberEntity
import org.example.junglebook.enums.Ideology
import org.example.junglebook.model.Member
import org.example.junglebook.repository.MemberCampHistoryRepository
import org.example.junglebook.repository.MemberRepository
import org.example.junglebook.util.toBoolean
import org.example.junglebook.web.dto.MemberDetailResponse
import org.example.junglebook.web.dto.SignUpRequest
import org.springframework.security.authentication.InternalAuthenticationServiceException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val memberCampHistoryRepository: MemberCampHistoryRepository
): UserDetailsService {
    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(loginId: String): UserDetails {
        return try {
            Member.from(findActivateMemberByLoginId(loginId))
        } catch (e: GlobalException) {
            // Spring Security가 처리할 수 있도록 UsernameNotFoundException으로 변환
            throw UsernameNotFoundException(e.code.description, e)
        }
    }

    fun existsByLoginId(loginId: String): Boolean = memberRepository.findByLoginId(loginId) != null

    @Transactional(readOnly = true)
    fun myInfoById(id: Long): MemberDetailResponse {
        val member = findActivateMemberById(id)
        val memberId = member.id ?: throw GlobalException(DefaultErrorCode.SYSTEM_ERROR)
        return MemberDetailResponse(
            id = memberId,
            loginId = member.loginId,
            name = member.name,
            email = member.email,
            phoneNumber = member.phoneNumber,
            nickname = member.nickname,
            birth = member.birth,
            sex = member.sex,
            ideology = member.ideology,
            profile =  member.profileImage,
            memberType = member.memberType,
            socialProvider = member.socialProvider,
            createdAt = member.createdAt
        )
    }
    @Transactional
    fun signUp(request: SignUpRequest) {
        validateSignUpRequest(request)
        val memberEntity = request.toMemberEntity()
        memberRepository.save(memberEntity)
    }

    fun validateSignUpRequest(request: SignUpRequest) {
        if (findByEmail(request.email) != null) {
            throw GlobalException(DefaultErrorCode.EMAIL_ALREADY_EXIST)
        }

        if (findByNickname(request.nickname) != null) {
            throw GlobalException(DefaultErrorCode.NICKNAME_ALREADY_EXIST)
        }

        if (existsByLoginId(request.loginId)) {
            throw GlobalException(DefaultErrorCode.LOGIN_ID_ALREADY_EXIST)
        }
    }

    fun findActivateMemberByLoginId(loginId: String): MemberEntity =
        memberRepository.findByLoginId(loginId)?.let { member ->
            if (member.deleteYn.toBoolean()) throw GlobalException(DefaultErrorCode.DELETED_MEMBER)
            member
        } ?: throw GlobalException(DefaultErrorCode.LOGIN_FAILURE)

    fun findActivateMemberById(id: Long): MemberEntity =
        memberRepository.findById(id).orElseThrow { 
            GlobalException(DefaultErrorCode.MEMBER_NOT_FOUND)
        }.let { member ->
            if (member.deleteYn.toBoolean()) throw GlobalException(DefaultErrorCode.DELETED_MEMBER)
            member
        }

    @Transactional
    fun passwordUpdate(member: MemberEntity, encodedPassword: String) {
        member.password = encodedPassword
        member.updatedAt = LocalDateTime.now()
        memberRepository.save(member)
    }


    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun loginTest(loginId: String): MemberEntity {
        val memberEntity = memberRepository.findByLoginId(loginId) 
            ?: throw GlobalException(DefaultErrorCode.MEMBER_NOT_FOUND)

        val memberId = memberEntity.id ?: throw GlobalException(DefaultErrorCode.SYSTEM_ERROR)
        memberRepository.updateLoginTime(memberId)
        return memberEntity
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun changeIdeology(userId: Long, ideology: Ideology) {
        val memberEntity = findActivateMemberById(userId)

        if (memberEntity.ideology == ideology) {
            throw GlobalException(DefaultErrorCode.SAME_IDEOLOGY)
        }

        val latestHistories = memberCampHistoryRepository.findLatestByMemberId(userId)
        val now = LocalDateTime.now()
        latestHistories.firstOrNull()?.let { history ->
            if (history.createdAt.isAfter(now.minusMonths(6))) {
                throw GlobalException(DefaultErrorCode.UNREACHED_TIME_TO_CHANGE_IDEOLOGY)
            }
        }

        memberEntity.ideology = ideology
        memberEntity.updatedAt = now
        memberRepository.save(memberEntity)

        val ideologyHistory = MemberCampHistoryEntity(
            memberId = userId,
            camp = ideology.ordinal
        )
        memberCampHistoryRepository.save(ideologyHistory)
    }

    fun findByEmail(email: String): MemberEntity? = memberRepository.findByEmail(email)

    fun findByNickname(nickname: String): MemberEntity? = memberRepository.findByNickname(nickname)

    @Transactional
    fun updateMember(member: MemberEntity) {
        member.updatedAt = LocalDateTime.now()
        memberRepository.save(member)
    }

    @Transactional
    fun softDelete(memberId: Long) {
        val member = findActivateMemberById(memberId)
        member.deleteYn = 1
        member.updatedAt = LocalDateTime.now()
        memberRepository.save(member)
    }
}