package kr.co.minust.api.exception

import org.springframework.http.HttpStatus

enum class DefaultErrorCode(
    val httpStatus: HttpStatus,
    val code: String,
    val title: String,
    val description: String
) {
    EMAIL_ALREADY_EXIST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(),"회원가입 실패", "이메일이 이미 존재합니다."),
    LOGIN_FAILURE(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "로그인 실패", "잘못된 로그인 아이디 또는 비밀번호입니다."),
    DELETED_MEMBER(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value().toString(), "삭제 회원", "삭제된 회원입니다."),
    INCORRECT_PASSWORDD(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "잘못된 비밀번호", "비밀번호를 잘못 입력하였습니다."),

    ALREADY_EXISTS(HttpStatus.CONFLICT, HttpStatus.CONFLICT.value().toString(), "중복 요청", "이미 처리된 요청입니다."),
    WRONG_ACCESS(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.value().toString(), "접근 거부", "잘못된 접근입니다."),
    SAME_IDEOLOGY(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "동일한 정치성향", "현재와 동일한 정치성향입니다."),
    UNREACHED_TIME_TO_CHANGE_IDEOLOGY(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "변경 제한", "정치성향 변경은 6개월 후에 가능합니다."),
    REPLY_EXISTS(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "댓글 존재", "댓글이 존재하여 삭제할 수 없습니다."),
    REPLY_NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value().toString(), "댓글 없음", "댓글을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value().toString(), "사용자 없음", "사용자를 찾을 수 없습니다."),
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value().toString(), "시스템 오류", "시스템 오류가 발생했습니다.")
    ;
    ;

    override fun toString(): String {
        return "$code:$title"
    }
}