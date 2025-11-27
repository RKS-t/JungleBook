package org.example.junglebook.exception

import org.springframework.http.HttpStatus

class InvalidTokenException(
    val status: HttpStatus,
    message: String
) : RuntimeException(message)