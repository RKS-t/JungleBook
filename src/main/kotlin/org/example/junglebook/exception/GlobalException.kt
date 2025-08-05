package kr.co.minust.api.exception

class GlobalException(
    val code: DefaultErrorCode,
    val errorMessage: String?,
    val description: String?
): RuntimeException(code.description) {
    constructor(code: DefaultErrorCode): this(code, code.title, code.description)

    constructor(code: DefaultErrorCode, message: String): this(code, message, null)
}
