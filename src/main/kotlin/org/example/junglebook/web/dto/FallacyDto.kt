package org.example.junglebook.web.dto

data class FallacyDetectionRequest(
    val text: String,
    val language: String = "ko",
    val topicTitle: String? = null,
    val topicDescription: String? = null
)

data class FallacyDetectionResponse(
    val hasFallacy: Boolean,
    val fallacyType: String?,
    val confidence: Double,
    val explanation: String
)

data class FallacyAppealRequest(
    val appealReason: String
)

data class FallacyAppealResponse(
    val id: Long,
    val argumentId: Long,
    val appealerId: Long,
    val appealReason: String,
    val status: String,
    val createdAt: String
)

