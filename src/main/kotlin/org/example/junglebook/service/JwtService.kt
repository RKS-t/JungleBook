package org.example.junglebook.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.junglebook.constant.JBConstants
import org.example.junglebook.exception.InvalidTokenException
import org.example.junglebook.model.JwtPayload
import org.example.junglebook.model.Member
import org.example.junglebook.util.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService {

    @Value("\${jwt.signing.key}")
    private val jwtSigningKey: String = ""

    companion object {
        private const val MILLISECONDS_PER_SECOND = 1000L
        private const val SECONDS_PER_MINUTE = 60L
        private const val MINUTES_PER_HOUR = 60L
        private const val HOURS_PER_DAY = 24L
        private const val MILLISECONDS_PER_DAY = 
            MILLISECONDS_PER_SECOND * SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY
    }

    fun extract(claims: Claims): JwtPayload {
        return Json.decodeFromString<JwtPayload>(claims.subject)
    }

    fun extractAccessToken(token: String): JwtPayload {
        val jwtPayload = extract(extractAllClaims(token))
        
        require(jwtPayload.isAccessToken) {
            logger().warn("Invalid token type: expected Access Token. token: {}", token)
            InvalidTokenException(HttpStatus.BAD_REQUEST, "Invalid token type: expected Access Token")
        }

        return jwtPayload
    }

    fun extractRefreshToken(token: String): JwtPayload {
        val jwtPayload = extract(extractAllClaims(token))
        
        require(!jwtPayload.isAccessToken) {
            logger().warn("Invalid token type: expected Refresh Token. token: {}", token)
            InvalidTokenException(HttpStatus.BAD_REQUEST, "Invalid token type: expected Refresh Token")
        }

        return jwtPayload
    }

    fun generateToken(member: Member, isAccessToken: Boolean): String {
        val expireDays = if (isAccessToken) {
            JBConstants.ACCESS_TOKEN_EXPIRE_DAYS
        } else {
            JBConstants.REFRESH_TOKEN_EXPIRE_DAYS
        }

        val currentTimeMillis = System.currentTimeMillis()
        val expirationTimeMillis = currentTimeMillis + (expireDays * MILLISECONDS_PER_DAY)

        val jwtPayload = member.toJwtPayload(isAccessToken)
        val payloadJson = Json.encodeToString(jwtPayload)

        return Jwts.builder()
            .claims(mapOf<String, Any>())
            .subject(payloadJson)
            .issuedAt(Date(currentTimeMillis))
            .expiration(Date(expirationTimeMillis))
            .signWith(getSigningKey())
            .compact()
    }

    fun extractAllClaims(token: String): Claims {
        return try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            logger().info("Expired JWT token. token: {}", token, e)
            throw InvalidTokenException(HttpStatus.UNAUTHORIZED, "Expired token")
        } catch (e: SignatureException) {
            logger().warn("Invalid JWT signature. token: {}", token, e)
            throw InvalidTokenException(HttpStatus.BAD_REQUEST, "Invalid token signature")
        } catch (e: Exception) {
            logger().warn("Token validation failed. token: {}", token, e)
            throw InvalidTokenException(HttpStatus.BAD_REQUEST, "Invalid token")
        }
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            extractAllClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getSigningKey(): SecretKey {
        val keyBytes = Decoders.BASE64.decode(jwtSigningKey)
        return Keys.hmacShaKeyFor(keyBytes)
    }
}
