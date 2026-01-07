package org.example.junglebook.config

import jakarta.servlet.http.HttpServletResponse
import org.example.junglebook.filter.JwtAuthenticationFilter
import org.example.junglebook.service.MemberService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val userDetailsService: MemberService
) {

    @Value("\${http.permit.uris}")
    private val permitUris: String = ""

    companion object {
        private val PUBLIC_ENDPOINTS = arrayOf(
            "/api/login",
            "/api/signup",
            "/api/signup-and-login",
            "/api/social-login",
            "/api/social-signup",
            "/api/sign-in-by-refresh-token",
            "/api/check/**",
            "/api/health",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/actuator/health",
            "/error"
        )

        private val ALLOWED_ORIGINS = listOf(
            "http://localhost:3000",
            "http://localhost:8080",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:8080"
        )

        private val ALLOWED_METHODS = listOf(
            "POST", "GET", "DELETE", "PUT", "PATCH", "OPTIONS", "HEAD"
        )

        private val ALLOWED_HEADERS = listOf(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        )

        private val EXPOSED_HEADERS = listOf("Authorization")

        private const val CORS_MAX_AGE = 3600L
        private const val BCRYPT_ROUNDS = 12
    }

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(
        http: HttpSecurity,
        passwordEncoder: PasswordEncoder
    ): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(*PUBLIC_ENDPOINTS)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/debate/topics")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/debate/topics/hot")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/debate/topics/category/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/debate/topics/ongoing")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/debate/topics/ending-soon")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/debate/topics/{topicId}")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/debate/topics/search")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/board")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint { request, response, authException ->
                    handleUnauthorizedException(response, authException)
                }
            }
            .authenticationProvider(authenticationProvider(passwordEncoder))
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val corsConfiguration = CorsConfiguration().apply {
            allowedOrigins = ALLOWED_ORIGINS
            allowedMethods = ALLOWED_METHODS
            allowedHeaders = ALLOWED_HEADERS
            exposedHeaders = EXPOSED_HEADERS
            allowCredentials = true
            maxAge = CORS_MAX_AGE
        }

        val corsSource = UrlBasedCorsConfigurationSource()
        corsSource.registerCorsConfiguration("/**", corsConfiguration)
        return corsSource
    }

    @Bean
    @Throws(Exception::class)
    fun authenticationManager(
        config: AuthenticationConfiguration
    ): AuthenticationManager {
        return config.authenticationManager
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(
            BCryptPasswordEncoder.BCryptVersion.`$2A`,
            BCRYPT_ROUNDS
        )
    }

    @Bean
    fun authenticationProvider(
        passwordEncoder: PasswordEncoder
    ): AuthenticationProvider {
        return DaoAuthenticationProvider().apply {
            setUserDetailsService(userDetailsService)
            setPasswordEncoder(passwordEncoder)
        }
    }

    private fun handleUnauthorizedException(
        response: HttpServletResponse,
        authException: org.springframework.security.core.AuthenticationException
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        
        val errorResponse = """
            {
                "error": "Unauthorized",
                "message": "${authException.message}"
            }
        """.trimIndent()
        
        response.writer.write(errorResponse)
    }

    private fun getPermitUris(): Array<String> {
        return permitUris
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toTypedArray()
    }
}
