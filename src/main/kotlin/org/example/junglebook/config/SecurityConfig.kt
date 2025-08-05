package org.example.junglebook.config

import org.example.junglebook.service.MemberService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer
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
    val jwtAuthenticationFilter: JwtAuthenticationFilter,
    val userDetailsService: MemberService
) {
    @Value("\${http.permit.uris}")
    val permitUris: String = ""

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity, passwordEncoder: PasswordEncoder): SecurityFilterChain {
        return http.csrf { obj: CsrfConfigurer<HttpSecurity> -> obj.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeHttpRequests({ request ->
                request
                    .requestMatchers(*getPermitUris())
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            })
            .sessionManagement { manager: SessionManagementConfigurer<HttpSecurity> ->
                manager.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            }
            .authenticationProvider(authenticationProvider(passwordEncoder))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    private fun getPermitUris(): Array<String> {
        return permitUris.split(",").toTypedArray()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = listOf("*")
        configuration.allowedMethods = listOf("POST", "GET", "DELETE", "PUT", "PATCH", "OPTIONS", "HEAD")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    @Throws(Exception::class)
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager = config.authenticationManager

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(BCryptPasswordEncoder.BCryptVersion.`$2A`, 12)

    @Bean
    fun authenticationProvider(passwordEncoder: PasswordEncoder): AuthenticationProvider = DaoAuthenticationProvider().apply {
        this.setUserDetailsService(userDetailsService)
        this.setPasswordEncoder(passwordEncoder)
    }
}