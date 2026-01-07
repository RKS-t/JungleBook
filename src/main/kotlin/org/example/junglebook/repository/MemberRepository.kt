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

    fun findByLoginId(loginId: String): MemberEntity?

    fun findByEmail(email: String): MemberEntity?

    fun findFirstByEmail(email: String): MemberEntity?

    fun findByNickname(nickname: String): MemberEntity?

    fun findBySocialProviderAndSocialProviderId(
        socialProvider: SocialProvider,
        socialProviderId: String
    ): MemberEntity?

    @Query("SELECT m FROM MemberEntity m WHERE m.deleteYn = 0")
    fun findAllActiveMembers(): List<MemberEntity>

    @Modifying
    @Query("UPDATE MemberEntity m SET m.updatedAt = CURRENT_TIMESTAMP WHERE m.id = :memberId")
    fun updateLoginTime(@Param("memberId") memberId: Long)

    fun existsByEmail(email: String): Boolean

    fun existsByLoginId(loginId: String): Boolean

    fun existsByNickname(nickname: String): Boolean

    @Query("SELECT m FROM MemberEntity m WHERE m.loginId = :loginId AND m.deleteYn = 0")
    fun findActiveByLoginId(@Param("loginId") loginId: String): MemberEntity?

    @Query("SELECT m FROM MemberEntity m WHERE m.id = :id AND m.deleteYn = 0")
    fun findActiveById(@Param("id") id: Long): MemberEntity?

    @Query("SELECT m FROM MemberEntity m WHERE m.email = :email AND m.deleteYn = 0")
    fun findActiveByEmail(@Param("email") email: String): MemberEntity?
}