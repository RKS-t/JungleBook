package org.example.junglebook.service

import kr.co.minust.api.exception.DefaultErrorCode
import kr.co.minust.api.exception.GlobalException
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
    override fun loadUserByUsername(loginId: String): UserDetails =
        Member.from(findActivateMemberByLoginId(loginId))

    fun myInfoByLoginId(loginId: String) = memberRepository.findByLoginId(loginId)

    @Transactional(readOnly = true)
    fun myInfoById(id: Long): MemberDetailResponse {
        val member = findActivateMemberById(id)
        return MemberDetailResponse(
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
            memberType = member.memberType,
            socialProvider = member.socialProvider,
            createdAt = member.createdAt
        )
    }
    @Transactional
    fun signUp(request: SignUpRequest) {
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


    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun loginTest(loginId: String): MemberEntity {
        val memberEntity = memberRepository.findByLoginId(loginId)
            ?: throw GlobalException(DefaultErrorCode.USER_NOT_FOUND)

        memberRepository.updateLoginTime(memberEntity.id!!)
        return memberEntity
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = [Exception::class])
    fun changeIdeology(userId: Long, ideology: Ideology) {
        val memberEntity = findActivateMemberById(userId)

        // 현재 이데올로지와 변경하려는 이데올로지가 동일한지 확인
        if (memberEntity.ideology == ideology) {
            throw GlobalException(DefaultErrorCode.SAME_IDEOLOGY)
        }

        // 6개월 제한 체크
        val latestHistory = memberCampHistoryRepository.findLatestByMemberId(userId)
        val now = LocalDateTime.now()
        latestHistory?.let { history ->
            if (history.createdAt.isAfter(now.minusMonths(6))) {
                throw GlobalException(DefaultErrorCode.UNREACHED_TIME_TO_CHANGE_IDEOLOGY)
            }
        }

        // 회원 정보 업데이트
        memberEntity.ideology = ideology
        memberEntity.updatedAt = now
        memberRepository.save(memberEntity)

        // 이데올로지 변경 히스토리 저장 (camp 필드를 ideology 값으로 매핑)
        val ideologyHistory = MemberCampHistoryEntity(
            memberId = userId,
            camp = ideology.ordinal // C=0, L=1, M=2, N=3
        )
        memberCampHistoryRepository.save(ideologyHistory)
    }

    // 편의 메서드들
    fun findByEmail(email: String): MemberEntity? {
        return memberRepository.findByEmail(email)
    }

    fun findByNickname(nickname: String): MemberEntity? {
        return memberRepository.findByNickname(nickname)
    }

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