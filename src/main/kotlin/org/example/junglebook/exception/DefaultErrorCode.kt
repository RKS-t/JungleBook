package kr.co.minust.api.exception

import org.springframework.http.HttpStatus

enum class DefaultErrorCode(
    val httpStatus: HttpStatus,
    val code: String,
    val title: String,
    val description: String
) {
    UNKNOWN_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value().toString(), "서버 에러", "서버 에러가 발생하였습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "잘못된 요청", "잘못된 요청으로 에러가 발생하였습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value().toString(), HttpStatus.NOT_FOUND.reasonPhrase, ""),

    EMAIL_ALREADY_EXIST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(),"회원가입 실패", "이메일이 이미 존재합니다."),
    LOGIN_FAILURE(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "로그인 실패", "잘못된 로그인 아이디 또는 비밀번호입니다."),
    SIGNUP_WAITING(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "회원가입 대기 중", "현재 회원가입이 대기 중입니다."),
    DELETED_MEMBER(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value().toString(), "삭제 회원", "삭제된 회원입니다."),
    //INVALID_PHONENUMBER(HttpStatus.BAD_REQUEST, "1000", "Invalid Phone Number", "The phone number is invalid."),
    INCORRECT_PASSWORDD(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "잘못된 비밀번호", "비밀번호를 잘못 입력하였습니다."),
    NOT_WORKING_DAY(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "출퇴근 에러", "출퇴근 날짜가 아닙니다."),
    LATE(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "지각처리", "출근 시간 이후에 출근하여\n지각으로 처리됩니다.\n근태관리 탭에서 정정신청 가능합니다."),
    EXCEEDS_DISTANCE(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value().toString(), "출퇴근 불가지역", "해당지역은 출퇴근 불가지역입니다.\n해당 좌표 300M 근처까지 이동해서 다시 시도해주세요.")
    ;

    override fun toString(): String {
        return "$code:$title"
    }
}