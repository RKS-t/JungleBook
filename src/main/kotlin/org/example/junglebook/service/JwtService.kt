package org.example.junglebook.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.junglebook.model.JwtPayload
import org.example.junglebook.model.Member
import org.example.junglebook.util.logger
import kr.co.minust.api.exception.InvalidTokenException

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey


@Service
class JwtService {
    @Value("\${jwt.signing.key}")
    val jwtSigningKey: String = ""

    fun extract(claims: Claims): JwtPayload = Json.decodeFromString<JwtPayload>(claims.subject)

    fun extractAccessToken(token: String): JwtPayload {
        val payload = this.extract(this.extractAllClaims(token))
        if (!payload.isAccessToken) {
            logger().warn("토큰 유효성 체크 중 Exception.\ntoken:{}", token)
            throw InvalidTokenException(HttpStatus.BAD_REQUEST, "invalid token")
        }

        return payload
    }

    fun extractRefreshToken(token: String): JwtPayload {
        val payload = this.extract(this.extractAllClaims(token))
        if (payload.isAccessToken) {
            logger().warn("토큰 유효성 체크 중 Exception.\ntoken:{}", token)
            throw InvalidTokenException(HttpStatus.BAD_REQUEST, "invalid token")
        }

        return payload
    }

    fun generateToken(member: Member, isAccessToken: Boolean): String {
        return Jwts.builder()
            .claims(mapOf<String, Any>())
            .subject(Json.encodeToString(member.toJwtPayload(isAccessToken)))
            .issuedAt(Date(member.issued))
            .expiration(Date(member.expired))
            .signWith(this.getSigningKey())
            .compact()
    }

    fun extractAllClaims(token: String): Claims =
        try {
            Jwts.parser()
                .verifyWith(this.getSigningKey())
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            logger().info("만료 된 토큰.\ntoken:{}", token, e)
            throw InvalidTokenException(HttpStatus.UNAUTHORIZED, "expired token")
        } catch (e: SignatureException) {
            logger().warn("Invalid JWT signature.\ntoken:{}", token, e)
            throw InvalidTokenException(HttpStatus.BAD_REQUEST, "invalid token")
        } catch (e: Exception) {
            logger().warn("토큰 유효성 체크 중 Exception.\ntoken:{}", token, e)
            throw InvalidTokenException(HttpStatus.BAD_REQUEST, "invalid token")
        }

    private fun getSigningKey(): SecretKey =
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSigningKey))
}