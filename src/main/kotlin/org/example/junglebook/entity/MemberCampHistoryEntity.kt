package org.example.junglebook.entity

import jakarta.persistence.*
import org.example.junglebook.enums.Ideology
import java.time.LocalDateTime

@Entity
@Table(name = "member_camp_history")
data class MemberCampHistoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "camp", nullable = false)
    val camp: Int, // 0=C(보수), 1=L(진보), 2=M(중도), 3=N(없음)

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // camp 값을 Ideology enum으로 변환하는 편의 메서드
    fun getCampAsIdeology(): Ideology {
        return when (camp) {
            0 -> Ideology.C
            1 -> Ideology.L
            2 -> Ideology.M
            3 -> Ideology.N
            else -> Ideology.N
        }
    }

    companion object {
        // Ideology enum을 camp 값으로 변환하는 편의 메서드
        fun ideologyToCampValue(ideology: Ideology): Int {
            return when (ideology) {
                Ideology.C -> 0
                Ideology.L -> 1
                Ideology.M -> 2
                Ideology.N -> 3
            }
        }
    }
}