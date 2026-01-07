package org.example.junglebook.util

import mu.KLogger
import mu.KotlinLogging
import org.example.junglebook.constant.JBConstants
import org.springframework.security.core.context.SecurityContext
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

private const val AES_TRANSFORMATION = "AES/ECB/PKCS5Padding"
private const val AES_ALGORITHM = "AES"
private const val AES_KEY = "4VyFmq4JXLDn1DMI2gYIc1Qh2uHqNEpX"

private val SECRET_KEY = SecretKeySpec(
    AES_KEY.toByteArray(StandardCharsets.UTF_8),
    AES_ALGORITHM
)

inline fun <reified T> T.logger(): KLogger {
    return KotlinLogging.logger(T::class.qualifiedName.orEmpty())
}

// Base64로 인코딩된 암호화 문자열 반환
fun String.aesEncrypt(): String {
    val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
        init(Cipher.ENCRYPT_MODE, SECRET_KEY)
    }
    
    val encryptedBytes = cipher.doFinal(this.toByteArray(StandardCharsets.UTF_8))
    return Base64.getEncoder().encodeToString(encryptedBytes)
}

// Base64로 인코딩된 문자열을 복호화하여 원본 문자열 반환
fun String.aesDecrypt(): String {
    val encryptedBytes = Base64.getDecoder().decode(this)
    val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
        init(Cipher.DECRYPT_MODE, SECRET_KEY)
    }
    
    val decryptedBytes = cipher.doFinal(encryptedBytes)
    return String(decryptedBytes, StandardCharsets.UTF_8)
}

fun Int.toBoolean(): Boolean {
    return this == 1
}

fun Boolean.toInt(): Int {
    return if (this) 1 else 0
}

fun SecurityContext.loginId(): String {
    return this.authentication.name
}

fun SecurityContext.role(): String? {
    return null
}

fun LocalDateTime.toEpochMilli(): Long {
    return this
        .atZone(ZoneId.of(JBConstants.ZONE_ID_SEOUL))
        .toInstant()
        .toEpochMilli()
}
