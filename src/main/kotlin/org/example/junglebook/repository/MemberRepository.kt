package org.example.junglebook.repository

import org.example.junglebook.entity.MemberEntity
import org.example.junglebook.enums.SocialProvider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<MemberEntity, Long> {

    // 로그인 ID로 회원 조회
    fun findByLoginId(loginId: String): MemberEntity?

    // 이메일로 회원 조회
    fun findByEmail(email: String): MemberEntity?

    // 이메일로 첫 번째 회원 조회 (중복 체크용)
    fun findFirstByEmail(email: String): MemberEntity?

    // 닉네임으로 회원 조회
    fun findByNickname(nickname: String): MemberEntity?

    // 소셜 제공자와 소셜 제공자 ID로 회원 조회
    fun findBySocialProviderAndSocialProviderId(
        socialProvider: SocialProvider,
        socialProviderId: String
    ): MemberEntity?

    // 활성 회원만 조회 (deleteYn = 0)
    @Query("SELECT m FROM MemberEntity m WHERE m.deleteYn = 0")
    fun findAllActiveMembers(): List<MemberEntity>

    // 특정 회원의 로그인 시간 업데이트
    @Modifying
    @Query("UPDATE MemberEntity m SET m.updatedAt = CURRENT_TIMESTAMP WHERE m.id = :memberId")
    fun updateLoginTime(@Param("memberId") memberId: Long)

    // 이메일 중복 체크
    fun existsByEmail(email: String): Boolean

    // 로그인 ID 중복 체크
    fun existsByLoginId(loginId: String): Boolean

    // 닉네임 중복 체크
    fun existsByNickname(nickname: String): Boolean

    // 활성 회원 중에서 로그인 ID로 조회
    @Query("SELECT m FROM MemberEntity m WHERE m.loginId = :loginId AND m.deleteYn = 0")
    fun findActiveByLoginId(@Param("loginId") loginId: String): MemberEntity?

    // 활성 회원 중에서 ID로 조회
    @Query("SELECT m FROM MemberEntity m WHERE m.id = :id AND m.deleteYn = 0")
    fun findActiveById(@Param("id") id: Long): MemberEntity?

    // 활성 회원 중에서 이메일로 조회
    @Query("SELECT m FROM MemberEntity m WHERE m.email = :email AND m.deleteYn = 0")
    fun findActiveByEmail(@Param("email") email: String): MemberEntity?
}