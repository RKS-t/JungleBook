package org.example.junglebook.constant

import java.time.format.DateTimeFormatter

class JBConstants {
    companion object {
        const val BEARER = "Bearer "
        const val NOT_DELETED = 0
        const val DELETED = 1

        const val ZONE_ID_SEOUL = "Asia/Seoul"
        const val DEFAULT_EXPIRE_DAYS = 7L
        const val ACCESS_TOKEN_EXPIRE_DAYS = 1L
        const val REFRESH_TOKEN_EXPIRE_DAYS = 30L

        const val AmazonS3PublicUrl = "https://%s.s3.%s.amazonaws.com/%s"

        const val DEBATE_ARGUMENT_MAX_CONTENT_LENGTH = 5000
        const val DEBATE_HOT_TOPIC_THRESHOLD = 100
        const val DEBATE_HOT_TOPIC_VIEW_COUNT_DIVISOR = 10
        const val DEBATE_TOP_ARGUMENTS_LIMIT = 3
        const val DEBATE_POPULAR_ARGUMENTS_LIMIT = 5
        const val DEBATE_RECENT_WEEKS = 7
        const val DEBATE_DASHBOARD_TOPICS_LIMIT = 5
        const val DEBATE_DEFAULT_HOT_TOPICS_LIMIT = 10
        const val DEBATE_DEFAULT_ENDING_SOON_LIMIT = 10
        const val DEBATE_DEFAULT_POPULAR_REPLIES_LIMIT = 5

        val FORMATTER_YYYYMM = DateTimeFormatter.ofPattern("yyyyMM")
        val FORMATTER_YYYYMMDD = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val FORMATTER_YYYYMMDD2 = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}