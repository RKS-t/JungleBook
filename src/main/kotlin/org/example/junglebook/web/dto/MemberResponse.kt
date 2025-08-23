package org.example.junglebook.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import org.example.junglebook.entity.MemberEntity
import org.example.junglebook.enums.Ideology
import org.example.junglebook.enums.Sex
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MemberResponse(
    val memberId: Long?,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val birth: String,
    val sex: Sex,
    val ideology: Ideology,
    val loginId: String,
    val nickname: String,
    val profileImage: String?,
    val deleteYn: Int,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: LocalDateTime,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun of(entity: MemberEntity?): MemberResponse? {
            return entity?.let {
                MemberResponse(
                    memberId = it.id,
                    name = it.name,
                    email = it.email,
                    phoneNumber = it.phoneNumber,
                    birth = it.birth,
                    sex = it.sex,
                    ideology = it.ideology,
                    loginId = it.loginId,
                    nickname = it.nickname,
                    profileImage = it.profileImage,
                    deleteYn = it.deleteYn,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
        }
    }
}