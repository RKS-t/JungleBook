package org.example.junglebook.repository

import org.example.junglebook.entity.MemberEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository: JpaRepository<MemberEntity, Long> {
    fun findByLoginId(loginId: String): MemberEntity?

    fun findFirstByEmail(email: String): MemberEntity?
}