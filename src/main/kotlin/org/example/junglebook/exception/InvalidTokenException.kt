package kr.co.minust.api.exception

import org.springframework.http.HttpStatus

class InvalidTokenException(val status: HttpStatus, override val message: String): RuntimeException() {
}