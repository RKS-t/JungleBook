package org.example.junglebook.exception

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(GlobalException::class)
    fun handleGlobalException(
        exception: GlobalException,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        val status = exception.code.httpStatus
        val errorResponse = mapOf<String, Any>(
            "error" to exception.code.title,
            "message" to (exception.errorMessage ?: exception.code.description),
            "code" to exception.code.code,
            "path" to request.requestURI
        )
        
        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidTokenException(
        exception: InvalidTokenException,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        val errorResponse = mapOf<String, Any>(
            "error" to "Invalid Token",
            "message" to (exception.message ?: "Invalid token"),
            "status" to exception.status.value(),
            "path" to request.requestURI
        )
        
        return ResponseEntity.status(exception.status).body(errorResponse)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        exception: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        val errorResponse = mapOf<String, Any>(
            "error" to "Invalid Request",
            "message" to (exception.message ?: "잘못된 요청입니다."),
            "code" to HttpStatus.BAD_REQUEST.value().toString(),
            "path" to request.requestURI
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(
        exception: IllegalStateException,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        val errorResponse = mapOf<String, Any>(
            "error" to "Invalid State",
            "message" to (exception.message ?: "잘못된 상태입니다."),
            "code" to HttpStatus.BAD_REQUEST.value().toString(),
            "path" to request.requestURI
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
}

