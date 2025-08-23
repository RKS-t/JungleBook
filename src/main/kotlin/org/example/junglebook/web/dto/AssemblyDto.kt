package org.example.junglebook.web.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import org.example.junglebook.entity.assembly.AssemblyBillEntity
import org.example.junglebook.entity.assembly.AssemblyVoteEntity

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AssemblyBillResponse(
    val totalCount: Int,
    val pageNo: Int,
    val assemblyBills: List<AssemblyBill>
) {
    companion object {
        fun of(totalCount: Int, pageNo: Int, entities: List<AssemblyBillEntity>?): AssemblyBillResponse {
            return AssemblyBillResponse(
                totalCount = totalCount,
                pageNo = pageNo,
                assemblyBills = entities?.map { AssemblyBill.of(it) } ?: emptyList()
            )
        }
    }

    data class AssemblyBill(
        val billId: String?,
        val billNo: String?,
        val billName: String?,
        val billKind: String?,
        val committeeName: String?,
        val proposer: String?,
        val proposeDt: String?,
        val voteTcnt: Int,
        val yesTcnt: Int,
        val noTcnt: Int,
        val blankTcnt: Int,
        val procResult: String?,
        val link: String?,
        val age: Int
    ) {
        companion object {
            fun of(entity: AssemblyBillEntity): AssemblyBill {
                return AssemblyBill(
                    billId = entity.billId,
                    billNo = entity.billNo,
                    billName = entity.billName,
                    billKind = entity.billKind,
                    committeeName = entity.committeeName,
                    proposer = entity.proposer,
                    proposeDt = entity.proposeDt,
                    voteTcnt = entity.voteTcnt,
                    yesTcnt = entity.yesTcnt,
                    noTcnt = entity.noTcnt,
                    blankTcnt = entity.blankTcnt,
                    procResult = entity.procResult,
                    link = entity.link,
                    age = entity.age
                )
            }
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AssemblyVoteResponse(
    val totalCount: Int,
    val pageNo: Int,
    val assemblyVotes: List<AssemblyVote>
) {
    companion object {
        fun of(totalCount: Int, pageNo: Int, entities: List<AssemblyVoteEntity>?): AssemblyVoteResponse {
            return AssemblyVoteResponse(
                totalCount = totalCount,
                pageNo = pageNo,
                assemblyVotes = entities?.map { AssemblyVote.of(it) } ?: emptyList()
            )
        }
    }

    data class AssemblyVote(
        @JsonIgnore
        val billId: String?,
        @JsonIgnore
        val memoNo: String?,
        val koName: String?,
        @JsonIgnore
        val cnName: String?,
        @JsonIgnore
        val polyCd: String?,
        val polyNm: String?,
        @JsonIgnore
        val voteDate: String?,
        val voteMod: String?
    ) {
        companion object {
            fun of(entity: AssemblyVoteEntity): AssemblyVote {
                return AssemblyVote(
                    billId = entity.billId,
                    memoNo = entity.memoNo,
                    koName = entity.koName,
                    cnName = entity.cnName,
                    polyCd = entity.polyCd,
                    polyNm = entity.polyNm,
                    voteDate = entity.voteDate,
                    voteMod = entity.voteMod
                )
            }
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AssemblyPolyVoteResponse(
    val totalCount: Int,
    val pageNo: Int,
    val assemblyVotes: List<AssemblyPolyVote>
) {
    companion object {
        fun of(totalCount: Int, pageNo: Int, entities: List<AssemblyVoteEntity>?): AssemblyPolyVoteResponse {
            return AssemblyPolyVoteResponse(
                totalCount = totalCount,
                pageNo = pageNo,
                assemblyVotes = entities?.map { AssemblyPolyVote.of(it) } ?: emptyList()
            )
        }
    }

    data class AssemblyPolyVote(
        val polyNm: String?,
        val voteMod: String?,
        val polyCnt: Int
    ) {
        companion object {
            fun of(entity: AssemblyVoteEntity): AssemblyPolyVote {
                return AssemblyPolyVote(
                    polyNm = entity.polyNm,
                    voteMod = entity.voteMod,
                    polyCnt = entity.polyCnt
                )
            }
        }
    }
}