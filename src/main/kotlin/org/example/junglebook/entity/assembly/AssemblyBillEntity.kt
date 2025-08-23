package org.example.junglebook.entity.assembly

import jakarta.persistence.*

@Entity
@Table(name = "assembly_bill")
data class AssemblyBillEntity(
    @Id
    @Column(name = "bill_id", length = 50)
    val billId: String,

    @Column(name = "bill_no", length = 100)
    val billNo: String? = null,

    @Column(name = "bill_name", length = 500)
    val billName: String? = null,

    @Column(name = "bill_kind", length = 50)
    val billKind: String? = null,

    @Column(name = "committee_name", length = 100)
    val committeeName: String? = null,

    @Column(name = "proposer", length = 200)
    val proposer: String? = null,

    @Column(name = "propose_dt", length = 10)
    val proposeDt: String? = null,

    @Column(name = "vote_tcnt")
    val voteTcnt: Int = 0,

    @Column(name = "yes_tcnt")
    val yesTcnt: Int = 0,

    @Column(name = "no_tcnt")
    val noTcnt: Int = 0,

    @Column(name = "blank_tcnt")
    val blankTcnt: Int = 0,

    @Column(name = "proc_result", length = 50)
    val procResult: String? = null,

    @Column(name = "link", length = 1000)
    val link: String? = null,

    @Column(name = "age")
    val age: Int = 0
)