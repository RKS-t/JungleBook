package org.example.junglebook.web.dto

data class CountResponse(
    val count: Int
) {
    companion object {
        fun of(count: Int): CountResponse {
            return CountResponse(count)
        }
    }
}