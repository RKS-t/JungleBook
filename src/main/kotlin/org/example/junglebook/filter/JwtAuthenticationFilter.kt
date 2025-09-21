package org.example.junglebook.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.minust.api.exception.InvalidTokenException
import org.example.junglebook.constant.JBConstants
import org.example.junglebook.service.JwtService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class JwtAuthenticationFilter(val jwtService: JwtService): OncePerRequestFilter() {
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (authHeader == null || !authHeader.startsWith(JBConstants.Companion.BEARER) || authHeader.substring(7).isEmpty()) {
            filterChain.doFilter(request, response)
            return
        }

        if (SecurityContextHolder.getContext().authentication == null) {
            try {


                val token = authHeader.substring(7)
                val accessToken = jwtService.extractAccessToken(token)
                val authToken = UsernamePasswordAuthenticationToken(
                    accessToken.loginId,
                    null,
                    emptyList()
                ).apply {
                    details = WebAuthenticationDetailsSource().buildDetails(request)
                }
                val context = SecurityContextHolder.createEmptyContext().apply {
                    authentication = authToken
                }
                SecurityContextHolder.setContext(context)
                filterChain.doFilter(request, response)
            } catch (exception: Exception) {
                when (exception) {
                    is InvalidTokenException -> {
                        response.status = exception.status.value()
                    }
                    else -> {
                        response.status = HttpServletResponse.SC_BAD_REQUEST
                    }
                }

                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.writer.write("{\"error\":\"${exception.message}\"}")
            }
        }
    }
}