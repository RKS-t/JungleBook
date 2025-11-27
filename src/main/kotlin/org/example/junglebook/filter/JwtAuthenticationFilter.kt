package org.example.junglebook.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.junglebook.constant.JBConstants
import org.example.junglebook.exception.InvalidTokenException
import org.example.junglebook.model.Member
import org.example.junglebook.service.JwtService
import org.example.junglebook.service.MemberService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val memberService: MemberService
) : OncePerRequestFilter() {

    companion object {
        private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
        
        private const val BEARER_PREFIX_LENGTH = 7
        
        private val PERMIT_ALL_PATHS = listOf(
            "/api/login",
            "/api/signup",
            "/api/signup-and-login",
            "/api/social-login",
            "/api/social-signup",
            "/api/sign-in-by-refresh-token",
            "/api/health",
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars",
            "/actuator/health",
            "/error"
        )
        
        private val PERMIT_ALL_PATH_PREFIXES = listOf(
            "/api/check/",
            "/swagger-ui/",
            "/v3/api-docs/",
            "/swagger-resources/",
            "/webjars/"
        )
    }

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestPath = request.requestURI

        if (isPermitAllPath(requestPath)) {
            logger.debug("PermitAll path detected: $requestPath - skipping JWT authentication")
            filterChain.doFilter(request, response)
            return
        }

        val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        
        if (!isValidAuthorizationHeader(authorizationHeader)) {
            logger.debug("No valid Authorization header found for path: $requestPath - delegating to Spring Security")
            filterChain.doFilter(request, response)
            return
        }

        val token = extractTokenFromHeader(authorizationHeader)
        if (token.isEmpty()) {
            logger.debug("Empty token found for path: $requestPath - delegating to Spring Security")
            filterChain.doFilter(request, response)
            return
        }

        if (SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            authenticateWithToken(token, request)
            filterChain.doFilter(request, response)
        } catch (exception: Exception) {
            handleAuthenticationException(exception, requestPath, response)
        }
    }

    private fun isPermitAllPath(path: String): Boolean {
        if (PERMIT_ALL_PATHS.contains(path)) {
            return true
        }
        return PERMIT_ALL_PATH_PREFIXES.any { path.startsWith(it) }
    }

    private fun isValidAuthorizationHeader(authorizationHeader: String?): Boolean {
        return authorizationHeader != null && 
               authorizationHeader.startsWith(JBConstants.BEARER)
    }

    private fun extractTokenFromHeader(authorizationHeader: String): String {
        return authorizationHeader.substring(BEARER_PREFIX_LENGTH).trim()
    }

    private fun authenticateWithToken(token: String, request: HttpServletRequest) {
        val jwtPayload = jwtService.extractAccessToken(token)
        val memberEntity = memberService.findActivateMemberByLoginId(jwtPayload.loginId)
        val member = Member.from(memberEntity)
        val authorities = getAuthoritiesForUser(jwtPayload.loginId)

        val authenticationToken = UsernamePasswordAuthenticationToken(
            member,
            null,
            authorities
        ).apply {
            details = WebAuthenticationDetailsSource().buildDetails(request)
        }

        val securityContext = SecurityContextHolder.createEmptyContext().apply {
            authentication = authenticationToken
        }
        
        SecurityContextHolder.setContext(securityContext)
        logger.debug("Authentication successful for user: ${jwtPayload.loginId}")
    }

    private fun handleAuthenticationException(
        exception: Exception,
        requestPath: String,
        response: HttpServletResponse
    ) {
        logger.error("JWT authentication failed for path: $requestPath", exception)

        val httpStatus = when (exception) {
            is InvalidTokenException -> exception.status.value()
            else -> HttpServletResponse.SC_UNAUTHORIZED
        }
        response.status = httpStatus

        val errorMessage = when (exception) {
            is InvalidTokenException -> exception.message ?: "Invalid token"
            else -> "Authentication failed"
        }

        response.contentType = MediaType.APPLICATION_JSON_VALUE
        try {
            response.writer.use { writer ->
                val errorResponse = """
                    {
                        "error": "$errorMessage",
                        "timestamp": "${java.time.LocalDateTime.now()}"
                    }
                """.trimIndent()
                writer.write(errorResponse)
                writer.flush()
            }
        } catch (e: Exception) {
            logger.error("Failed to write error response", e)
        }
    }

    // 현재는 기본 권한만 부여, 향후 RBAC 구현 시 확장 가능
    private fun getAuthoritiesForUser(loginId: String): List<GrantedAuthority> {
        return try {
            val authorities = mutableListOf<GrantedAuthority>()
            authorities.add(SimpleGrantedAuthority("ROLE_USER"))
            authorities
        } catch (e: Exception) {
            logger.warn("Failed to load authorities for user: $loginId", e)
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        }
    }
}
