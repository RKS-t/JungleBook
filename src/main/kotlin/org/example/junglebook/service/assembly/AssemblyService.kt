package org.example.junglebook.service.assembly


import org.example.junglebook.entity.assembly.AssemblyBillEntity
import org.example.junglebook.entity.assembly.AssemblyVoteEntity
import org.example.junglebook.repository.assembly.AssemblyBillRepository
import org.example.junglebook.repository.assembly.AssemblyVoteRepository
import org.example.junglebook.web.dto.AssemblyBillResponse
import org.example.junglebook.web.dto.AssemblyPolyVoteResponse
import org.example.junglebook.web.dto.AssemblyVoteResponse
import org.springframework.stereotype.Service

@Service
class AssemblyService(
    private val assemblyBillRepository: AssemblyBillRepository,
    private val assemblyVoteRepository: AssemblyVoteRepository
) {

    fun readAssemblyBill(billId: String?, billName: String?, proposer: String?): AssemblyBillResponse {
        val list = selectAssemblyBill(billId, billName, proposer)
        return AssemblyBillResponse.of(0, 0, list)
    }

    fun selectAssemblyBill(billId: String?, billName: String?, proposer: String?): List<AssemblyBillEntity> {
        return assemblyBillRepository.selectAssemblyBill(billId, billName, proposer)
    }

    fun readAssemblyVote(billId: String): AssemblyVoteResponse {
        val list = selectAssemblyVote(billId)
        return AssemblyVoteResponse.of(0, 0, list)
    }

    fun selectAssemblyVote(billId: String): List<AssemblyVoteEntity> {
        return assemblyVoteRepository.findByBillId(billId)
    }

    fun readAssemblyPolyVote(billId: String): AssemblyPolyVoteResponse {
        val list = selectAssemblyPolyVote(billId)
        return AssemblyPolyVoteResponse.of(0, 0, list)
    }

    fun selectAssemblyPolyVote(billId: String): List<AssemblyVoteEntity> {
        return assemblyVoteRepository.selectAssemblyPolyVote(billId)
    }
}