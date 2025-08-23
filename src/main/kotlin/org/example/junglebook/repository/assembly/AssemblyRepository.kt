package org.example.junglebook.repository.assembly


import org.example.junglebook.entity.assembly.AssemblyBillEntity
import org.example.junglebook.entity.assembly.AssemblyVoteEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AssemblyBillRepository : JpaRepository<AssemblyBillEntity, String> {

    // selectAssemblyBill 메서드 대체 - 동적 조건 검색
    @Query("SELECT a FROM AssemblyBillEntity a WHERE " +
            "(:billId IS NULL OR a.billId = :billId) AND " +
            "(:billName IS NULL OR a.billName LIKE %:billName%) AND " +
            "(:proposer IS NULL OR a.proposer LIKE %:proposer%)")
    fun selectAssemblyBill(
        @Param("billId") billId: String?,
        @Param("billName") billName: String?,
        @Param("proposer") proposer: String?
    ): List<AssemblyBillEntity>
}

@Repository
interface AssemblyVoteRepository : JpaRepository<AssemblyVoteEntity, Long> {

    // selectAssemblyVote 메서드 대체
    fun findByBillId(billId: String): List<AssemblyVoteEntity>

    // selectAssemblyPolyVote 메서드 대체
    @Query("SELECT v FROM AssemblyVoteEntity v WHERE v.billId = :billId ORDER BY v.polyNm")
    fun selectAssemblyPolyVote(@Param("billId") billId: String): List<AssemblyVoteEntity>
}