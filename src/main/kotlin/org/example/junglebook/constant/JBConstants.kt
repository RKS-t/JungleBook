package org.example.junglebook.constant

import java.time.format.DateTimeFormatter

class JBConstants {
    companion object {
        const val BEARER = "Bearer "
        const val NOT_DELETED = 0
        const val DELETED = 1

        const val ZONE_ID_SEOUL = "Asia/Seoul"
        const val DEFAULT_EXPIRE_DAYS = 7L

        const val AmazonS3PublicUrl = "https://%s.s3.%s.amazonaws.com/%s"

        val FORMATTER_YYYYMM = DateTimeFormatter.ofPattern("yyyyMM")
        val FORMATTER_YYYYMMDD = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val FORMATTER_YYYYMMDD2 = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}