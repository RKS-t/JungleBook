package org.example.junglebook.exception

import org.springframework.http.HttpStatus

enum class DefaultErrorCode(
    val httpStatus: HttpStatus,
    val code: String,
    val title: String,
    val description: String
) {
    EMAIL_ALREADY_EXIST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "이메일 중복", "이메일이 이미 존재합니다."),
    LOGIN_FAILURE(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "로그인 실패", "잘못된 로그인 아이디 또는 비밀번호입니다."),
    DELETED_MEMBER(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value().toString(), "삭제된 회원", "삭제된 회원입니다."),
    INCORRECT_PASSWORDD(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "잘못된 비밀번호", "비밀번호를 잘못 입력하였습니다."),

    ALREADY_EXISTS(HttpStatus.CONFLICT, HttpStatus.CONFLICT.value().toString(), "중복 요청", "이미 처리된 요청입니다."),
    WRONG_ACCESS(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.value().toString(), "접근 거부", "잘못된 접근입니다."),
    SAME_IDEOLOGY(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "동일한 정치성향", "현재와 동일한 정치성향입니다."),
    UNREACHED_TIME_TO_CHANGE_IDEOLOGY(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "변경 제한", "정치성향 변경은 6개월 후에 가능합니다."),
    REPLY_EXISTS(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "댓글 존재", "댓글이 존재하여 삭제할 수 없습니다."),
    REPLY_NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value().toString(), "댓글 없음", "댓글을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value().toString(), "사용자 없음", "사용자를 찾을 수 없습니다."),
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value().toString(), "시스템 오류", "시스템 오류가 발생했습니다."),

    LOGIN_ID_ALREADY_EXIST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "로그인 ID 중복", "로그인 ID가 이미 존재합니다."),
    NICKNAME_ALREADY_EXIST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "닉네임 중복", "닉네임이 이미 존재합니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "비밀번호 불일치", "새 비밀번호가 일치하지 않습니다."),

    SOCIAL_LOGIN_FAILURE(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "소셜 로그인 실패", "소셜 로그인 처리 중 오류가 발생했습니다."),
    SOCIAL_ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, HttpStatus.CONFLICT.value().toString(), "계정 연동 실패", "이미 소셜 계정이 연동되어 있습니다."),
    SOCIAL_ACCOUNT_LINKED_TO_OTHER(HttpStatus.CONFLICT, HttpStatus.CONFLICT.value().toString(), "계정 연동 실패", "이미 다른 계정에 연결된 소셜 계정입니다."),
    SOCIAL_ACCOUNT_NOT_LINKED(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "연동 해제 실패", "연결된 소셜 계정이 없습니다."),
    SOCIAL_ONLY_MEMBER_UNLINK_DENIED(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "연동 해제 불가", "소셜 전용 회원은 연동을 해제할 수 없습니다."),

    SOCIAL_MEMBER_PASSWORD_CHANGE_DENIED(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "비밀번호 변경 불가", "소셜 회원은 비밀번호를 변경할 수 없습니다."),
    CURRENT_PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "현재 비밀번호 오류", "현재 비밀번호가 일치하지 않습니다."),
    INVALID_SOCIAL_TOKEN(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.value().toString(), "유효하지 않은 소셜 토큰", "소셜 로그인 토큰이 유효하지 않습니다."),
    EXTERNAL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value().toString(), "외부 API 오류", "외부 API 호출 중 오류가 발생했습니다."),

    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.value().toString(), "토큰 만료", "인증 토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.value().toString(), "유효하지 않은 토큰", "인증 토큰이 유효하지 않습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.value().toString(), "리프레시 토큰 만료", "리프레시 토큰이 만료되었습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.value().toString(), "유효하지 않은 리프레시 토큰", "리프레시 토큰이 유효하지 않습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value().toString(), "회원 없음", "회원을 찾을 수 없습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.value().toString(), "인증 실패", "인증이 필요합니다."),
    INSUFFICIENT_PERMISSION(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.value().toString(), "권한 부족", "해당 작업을 수행할 권한이 없습니다."),
    
    // 댓글 관련 에러 코드
    REPLY_DEPTH_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "댓글 깊이 제한", "댓글 깊이 제한을 초과했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.value().toString(), "접근 거부", "해당 작업을 수행할 권한이 없습니다."),
    
    // 토론 관련 에러 코드
    DEBATE_TOPIC_MODIFY_DENIED(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.value().toString(), "토픽 수정 권한 없음", "토픽을 수정할 권한이 없습니다."),
    DEBATE_TOPIC_DELETE_DENIED(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.value().toString(), "토픽 삭제 권한 없음", "토픽을 삭제할 권한이 없습니다."),
    DEBATE_TOPIC_STATUS_CHANGE_DENIED(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.value().toString(), "토픽 상태 변경 권한 없음", "토픽 상태를 변경할 권한이 없습니다."),
    DEBATE_ARGUMENT_DELETE_DENIED(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.value().toString(), "논증 삭제 권한 없음", "논증을 삭제할 권한이 없습니다.");

    override fun toString(): String {
        return "$code:$title"
    }
}