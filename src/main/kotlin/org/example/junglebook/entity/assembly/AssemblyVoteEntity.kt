package org.example.junglebook.entity.assembly


import jakarta.persistence.*

@Entity
@Table(name = "assembly_vote")
data class AssemblyVoteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "bill_id", length = 50, nullable = false)
    val billId: String,

    @Column(name = "memo_no", length = 20)
    val memoNo: String? = null,

    @Column(name = "ko_name", length = 50)
    val koName: String? = null,

    @Column(name = "cn_name", length = 50)
    val cnName: String? = null,

    @Column(name = "poly_cd", length = 10)
    val polyCd: String? = null,

    @Column(name = "poly_nm", length = 50)
    val polyNm: String? = null,

    @Column(name = "vote_date", length = 10)
    val voteDate: String? = null,

    @Column(name = "vote_mod", length = 10)
    val voteMod: String? = null,

    @Column(name = "poly_cnt")
    val polyCnt: Int = 0
)