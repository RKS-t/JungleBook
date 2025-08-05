package kr.co.minust.api.exception

data class ErrorResponse(
    val status: String,
    val title: String? = null,
    val message: String? = null,
    val errors: List<FieldError>? = null
)

data class FieldError (
    val field: String? = null,
    val value: String? = null,
    val reason: Any? = null
)