package org.example.junglebook.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletResponse
import kr.co.minust.api.exception.DefaultErrorCode
import kr.co.minust.api.exception.GlobalException
import org.example.junglebook.constant.JBConstants
import org.example.junglebook.dto.LoginRequest
import org.example.junglebook.dto.MemberDto
import org.example.junglebook.dto.TokenDto
import org.example.junglebook.model.Member
import org.example.junglebook.properties.JwtTokenProperties
import org.example.junglebook.service.JwtService
import org.example.junglebook.service.MemberService
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime


@RestController
@RequestMapping
class MemberController(
    private val jwtService: JwtService,
    private val jwtTokenProperties: JwtTokenProperties,
    private val authenticationManager: AuthenticationManager,
    private val memberService: MemberService,
    private val passwordEncoder: PasswordEncoder
) {
    @Operation(
        summary = "로그인",
        description = "로그인 API"
    )
    @PostMapping("/api/login")
    fun signIn(@RequestBody loginRequest: LoginRequest, response: HttpServletResponse): ResponseEntity<TokenDto> =
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                loginRequest.loginId,
                loginRequest.password
            )
        ).let {
            val member = it.principal as Member
            ResponseEntity.ok(this.makeToken(member, response))
        }
    @Operation(
        summary = "로그인 by RefreshToken",
        description = "로그인 by RefreshToken API"
    )
    @PostMapping("/api/sign-in-by-refresh-token")
    fun signInByRefreshToken(
        @Parameter(hidden = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION) authorization: String,
        response: HttpServletResponse
    ): ResponseEntity<TokenDto> {
        val payload = jwtService.extractRefreshToken(authorization.substringAfter(JBConstants.BEARER))
        val member = memberService.findActivateMemberByLoginId(payload.loginId)
        return ResponseEntity.ok(this.makeTokenWithoutRefreshToken(Member.from(member), response))
    }

    private fun makeToken(member: Member, response: HttpServletResponse): TokenDto {
        val accessToken = jwtService.generateToken(member, true)
        val refreshToken = jwtService.generateToken(member.setExpired(jwtTokenProperties.refreshToken), false)
        response.setHeader(HttpHeaders.AUTHORIZATION, JBConstants.BEARER + accessToken)
        val info = memberService.myInfoByLoginId(member.loginId)
        return TokenDto(info!!.id!!, info.nickname, accessToken, refreshToken)
    }

    private fun makeTokenWithoutRefreshToken(member: Member, response: HttpServletResponse): TokenDto {
        val accessToken = jwtService.generateToken(member, true)
        response.setHeader(HttpHeaders.AUTHORIZATION, JBConstants.BEARER + accessToken)
        return TokenDto(accessToken = accessToken)
    }

    @PostMapping("/api/signup")
    fun signUp(@RequestBody request: MemberDto.SignUpRequest): ResponseEntity<Void> =
        request.apply {
            encodedPassword = passwordEncoder.encode(password)
        }.let {
            memberService.signUp(request)
            ResponseEntity.accepted().build()
        }

    @GetMapping("/api/id-check")
    fun isExist(@RequestParam loginId: String) = ResponseEntity.ok(memberService.myInfoByLoginId(loginId))

    @GetMapping("/api/member-info")
    fun memberInfo(@Parameter(hidden = true) memberId: Long) =
        memberService.myInfoById(memberId)

    @Operation(
        summary = "비밀번호 변경",
        description = "비밀번호 변경 API"
    )
    @PatchMapping("/api/password")
    fun changePassword(
        @Parameter(hidden = true) memberId: Long,
        @RequestBody request: MemberDto.MemberPasswordUpdateRequest
    ) {
        if (!request.isCorrect()) {
            throw GlobalException(DefaultErrorCode.INCORRECT_PASSWORDD)
        }

        val member = memberService.findActivateMemberById(memberId)
        if (!passwordEncoder.matches(request.password, member.password)) {
            throw GlobalException(DefaultErrorCode.INCORRECT_PASSWORDD)
        }
        val encodedPassword = passwordEncoder.encode(request.newPassword1)
        memberService.passwordUpdate(member,encodedPassword)
    }
}