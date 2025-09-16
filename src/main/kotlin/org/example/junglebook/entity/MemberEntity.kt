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
import org.example.junglebook.enums.MemberType
import org.example.junglebook.enums.Sex
import org.example.junglebook.enums.SocialProvider
import java.time.LocalDateTime

@Entity
@Table(name="member")
data class MemberEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    val id: Long? = null,

    @Column
    var name: String,

    @Column
    var birth: String,

    @Column
    var phoneNumber: String,

    @Column(unique = true)
    var email: String,

    @Column
    @Enumerated(EnumType.STRING)
    var sex: Sex,

    @Column
    @Enumerated(EnumType.STRING)
    var ideology: Ideology,

    @Column(unique = true)
    val loginId: String,

    @Column(unique = true)
    val nickname: String,

    @Column
    var password: String,

    @Column
    var profileImage: String,

    @Column
    var deleteYn: Int,

    @Column(updatable = false)
    val createdAt: LocalDateTime,

    @Column
    var updatedAt: LocalDateTime,

    //Oauth
    @Enumerated(EnumType.STRING)
    @Column
    var socialProvider: SocialProvider? = null,

    @Column
    var socialProviderId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column
    val memberType: MemberType = MemberType.REGULAR
)