package org.example.junglebook.repository

import org.example.junglebook.entity.MemberEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional


// Member Repository (기존 UserMapper 대체)
@Repository
interface MemberRepository : JpaRepository<MemberEntity, Long> {

    // 기존 함수들 (유지)
    fun findByLoginId(loginId: String): MemberEntity?

    fun findFirstByEmail(email: String): MemberEntity?

    // selectByKakaoId 대체 - 소셜 로그인 ID로 검색 (추후 확장 가능)
    @Query("SELECT m FROM MemberEntity m WHERE m.loginId = :socialId")
    fun findBySocialId(@Param("socialId") String socialId): MemberEntity?

    // selectByNaverId 대체 - 이메일로 검색 (네이버 연동시 이메일 사용)
    fun findByEmail(email: String): MemberEntity?

    // selectById 대체 (JpaRepository에서 기본 제공되지만 명시적으로 추가)
    override fun findById(memberId: Long): Optional<MemberEntity>

    // login 대체 - 로그인 시간 업데이트
    @Modifying
    @Query("UPDATE MemberEntity m SET m.updatedAt = CURRENT_TIMESTAMP WHERE m.id = :memberId")
    fun updateLoginTime(@Param("memberId") Long memberId): Int

    // 닉네임으로 검색
    fun findByNickname(nickname: String): MemberEntity?
}

// Member Camp History Entity (새로 생성 필요)
@Repository
interface MemberCampHistoryRepository : JpaRepository<MemberCampHistoryEntity, Long> {

    // selectLatestOneByUserId 대체
    @Query("SELECT h FROM MemberCampHistoryEntity h WHERE h.memberId = :memberId ORDER BY h.createdAt DESC LIMIT 1")
    fun findLatestByMemberId(@Param("memberId") Long memberId): MemberCampHistoryEntity?

    // insert는 JpaRepository의 save() 메서드 사용
}

// Member Given Tag Entity (새로 생성 필요)
@Repository
interface MemberGivenTagRepository : JpaRepository<MemberGivenTagEntity, Long> {

    // 특정 멤버의 태그 조회
    fun findByTagedMemberId(tagedMemberId: Long): List<MemberGivenTagEntity>

    // 특정 태그 조회
    fun findByTag(tag: String): List<MemberGivenTagEntity>

    // insert는 JpaRepository의 save() 메서드 사용
}

// Member Given Tag History Entity (새로 생성 필요)
@Repository
interface MemberGivenTagHistoryRepository : JpaRepository<MemberGivenTagHistoryEntity, Long> {

    // 특정 게시글의 태그 히스토리 조회
    fun findByPostTypeAndPostId(postType: BoardType, postId: Long): List<MemberGivenTagHistoryEntity>

    // 특정 멤버가 받은 태그 히스토리
    fun findByTagedMemberId(tagedMemberId: Long): List<MemberGivenTagHistoryEntity>

    // 특정 멤버가 준 태그 히스토리
    fun findByVoteMemberId(voteMemberId: Long): List<MemberGivenTagHistoryEntity>

    // insert는 JpaRepository의 save() 메서드 사용
}

// 필요한 Entity 클래스들 (새로 생성 필요)