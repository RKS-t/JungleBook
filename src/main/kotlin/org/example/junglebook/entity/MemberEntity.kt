package org.example.junglebook.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.example.junglebook.enums.Ideology
import org.example.junglebook.enums.Sex
import java.time.LocalDateTime

@Entity
@Table(name="member")
data class MemberEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    val id: Long? = null,

    @Column
    val name: String,

    @Column
    val birth: String,

    @Column
    val phoneNumber: String,

    @Column(unique = true)
    var email: String,

    @Column
    @Enumerated(EnumType.STRING)
    val sex: Sex,

    @Column
    @Enumerated(EnumType.STRING)
    val ideology: Ideology,

    @Column(unique = true)
    val loginId: String,

    @Column(unique = true)
    val nickname: String,

    @Column
    var password: String,

    @Column
    val profileImage: String,

    @Column
    var deleteYn: Int,

    @Column(updatable = false)
    val createdAt: LocalDateTime,

    @Column
    var updatedAt: LocalDateTime,
)