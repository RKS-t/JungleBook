package org.example.junglebook.exception

import org.springframework.http.HttpStatus

open class DataNotFoundException(val status: HttpStatus, override val message: String): RuntimeException() {
}