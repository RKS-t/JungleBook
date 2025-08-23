package org.example.junglebook.web.controller.assembly

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.example.junglebook.service.assembly.AssemblyService
import org.example.junglebook.web.dto.AssemblyBillResponse
import org.example.junglebook.web.dto.AssemblyPolyVoteResponse
import org.example.junglebook.web.dto.AssemblyVoteResponse
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@Tag(name = "주요법안")
@Validated
@RestController
@RequestMapping("/api/assembly")
class AssemblyController(
    private val assemblyService: AssemblyService
) {

    @Operation(summary = "법안목록")
    @GetMapping("/bill")
    fun readAssemblyBill(
        @RequestParam(name = "billId", required = false) billId: String?,
        @RequestParam(name = "billName", required = false) billName: String?,
        @RequestParam(name = "proposer", required = false) proposer: String?
    ): ResponseEntity<AssemblyBillResponse> {
        return ResponseEntity.ok(assemblyService.readAssemblyBill(billId, billName, proposer))
    }

    @Operation(summary = "찬반리스트")
    @GetMapping("/vote")
    fun readAssemblyVote(
        @RequestParam(name = "billId", required = true) billId: String
    ): ResponseEntity<AssemblyVoteResponse> {
        return ResponseEntity.ok(assemblyService.readAssemblyVote(billId))
    }

    @Operation(summary = "정당별 찬반")
    @GetMapping("/poly/vote")
    fun readAssemblyPolyVote(
        @RequestParam(name = "billId", required = true) billId: String
    ): ResponseEntity<AssemblyPolyVoteResponse> {
        return ResponseEntity.ok(assemblyService.readAssemblyPolyVote(billId))
    }
}