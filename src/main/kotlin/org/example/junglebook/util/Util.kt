package org.example.junglebook.util



import mu.KLogger
import mu.KotlinLogging
import org.example.junglebook.constant.JBConstants
import org.springframework.security.core.context.SecurityContext
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"
private const val AES_KEY = "4VyFmq4JXLDn1DMI2gYIc1Qh2uHqNEpX"
val SECRET_KEY = SecretKeySpec(AES_KEY.toByteArray(StandardCharsets.UTF_8), "AES")

inline fun <reified T> T.logger(): KLogger = KotlinLogging.logger(T::class.qualifiedName.orEmpty())

fun String.aesEncrypt(): String = Cipher.getInstance(TRANSFORMATION).apply {
    this.init(Cipher.ENCRYPT_MODE, SECRET_KEY)
}.let {
    Base64.getEncoder().encodeToString(it.doFinal(this.toByteArray(StandardCharsets.UTF_8)))
}

fun String.aesDecrypt(): String = Cipher.getInstance(TRANSFORMATION).apply {
    this.init(Cipher.DECRYPT_MODE, SECRET_KEY)
}.let {
    String(it.doFinal(Base64.getDecoder().decode(this)), StandardCharsets.UTF_8)
}

fun Int.toBoolean(): Boolean = this == 1

fun SecurityContext.loginId(): String = this.authentication.name

fun SecurityContext.role(): String? = null

fun LocalDateTime.toEpochMilli(): Long = this.atZone(ZoneId.of(JBConstants.ZONE_ID_SEOUL)).toInstant().toEpochMilli()

