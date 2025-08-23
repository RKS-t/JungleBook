package org.example.junglebook.web.dto

data class SuccessResponse(
    val success: Boolean
) {
    companion object {
        fun of(success: Boolean): SuccessResponse {
            return SuccessResponse(success)
        }

        // 편의 메서드들
        fun success(): SuccessResponse {
            return SuccessResponse(true)
        }

        fun failure(): SuccessResponse {
            return SuccessResponse(false)
        }
    }
}