package org.example.junglebook.exception

import org.springframework.http.HttpStatus

class UserNotFoundException(override val message: String): DataNotFoundException(status = HttpStatus.NOT_FOUND, message = message) {
    constructor(): this("User not found")
}