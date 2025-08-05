package kr.co.minust.api.exception

import org.springframework.http.HttpStatus

open class DataNotFoundException(val status: HttpStatus, override val message: String): RuntimeException() {
}