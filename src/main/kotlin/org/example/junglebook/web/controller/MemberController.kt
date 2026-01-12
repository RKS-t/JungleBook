package org.example.junglebook.web.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import jakarta.servlet.http.HttpServletResponse
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import org.example.junglebook.constant.JBConstants
import org.example.junglebook.enums.MemberType
import org.example.junglebook.enums.SocialProvider
import org.example.junglebook.web.dto.LoginRequest
import org.example.junglebook.web.dto.TokenDto
import org.example.junglebook.model.Member
import org.example.junglebook.properties.JwtTokenProperties
import org.example.junglebook.service.JwtService
import org.example.junglebook.service.KakaoOAuthService
import org.example.junglebook.service.MemberService
import org.example.junglebook.service.NaverOAuthService
import org.example.junglebook.service.SocialMemberService
import org.example.junglebook.web.dto.DuplicateCheckResponse
import org.example.junglebook.web.dto.LoginResponse
import org.example.junglebook.web.dto.MemberPasswordUpdateRequest
import org.example.junglebook.web.dto.SignUpRequest
import org.example.junglebook.web.dto.SocialLoginRequest
import org.example.junglebook.web.dto.SocialLoginTokenDto
import org.example.junglebook.web.dto.SocialSignUpCompleteRequest
import org.example.junglebook.web.dto.SocialSignUpRequest
import org.example.junglebook.web.dto.SocialUserInfo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@Api(tags = ["회원"])
@Validated
@RestController
@RequestMapping("/api")
class MemberController(
    private val jwtService: JwtService,
    private val jwtTokenProperties: JwtTokenProperties,
    private val authenticationManager: AuthenticationManager,
    private val memberService: MemberService,
    private val socialMemberService: SocialMemberService,
    private val kakaoOAuthService: KakaoOAuthService,
    private val naverOAuthService: NaverOAuthService
) {

    @ApiOperation("로그인")
    @PostMapping("/login")
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

    @ApiOperation("소셜 로그인")
    @PostMapping("/social-login")
    fun socialLogin(@RequestBody request: SocialLoginRequest, response: HttpServletResponse): ResponseEntity<SocialLoginTokenDto> {
        val socialUserInfo = getSocialUserInfo(request.provider, request.accessToken)
        val member = socialMemberService.findBySocialLogin(request.provider, socialUserInfo.id)
            ?.let { existingMember ->
                socialMemberService.updateSocialMember(
                    member = existingMember,
                    name = socialUserInfo.name,
                    email = socialUserInfo.email,
                    profileImage = socialUserInfo.profileImage
                )
            } ?: socialMemberService.createOrLinkSocialMember(
                name = socialUserInfo.name,
                email = socialUserInfo.email,
                profileImage = socialUserInfo.profileImage,
                provider = request.provider,
                providerId = socialUserInfo.id
            )
        return ResponseEntity.ok(createSocialLoginResponse(member, response))
    }

    @ApiOperation("리프레시 토큰으로 로그인")
    @PostMapping("/sign-in-by-refresh-token")
    fun signInByRefreshToken(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) authorization: String,
        response: HttpServletResponse
    ): ResponseEntity<TokenDto> {
        val payload = jwtService.extractRefreshToken(authorization.substringAfter(JBConstants.BEARER))
        val member = memberService.findActivateMemberByLoginId(payload.loginId)
        return ResponseEntity.ok(this.makeTokenWithoutRefreshToken(Member.from(member), response))
    }

    @ApiOperation("일반 회원가입")
    @PostMapping("/signup")
    fun signUp(@RequestBody request: SignUpRequest): ResponseEntity<Void> {
        memberService.signUp(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @ApiOperation("일반 회원가입 후 자동 로그인")
    @PostMapping("/signup-and-login")
    fun signUpAndLogin(@RequestBody request: SignUpRequest, response: HttpServletResponse): ResponseEntity<TokenDto> {
        memberService.signUp(request)

        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.loginId, request.password)
        )

        val member = authentication.principal as Member
        val tokenDto = makeToken(member, response)

        return ResponseEntity.status(HttpStatus.CREATED).body(tokenDto)
    }

    @ApiOperation("소셜 회원가입")
    @PostMapping("/social-signup")
    fun socialSignUp(@RequestBody request: SocialSignUpRequest, response: HttpServletResponse): ResponseEntity<SocialLoginTokenDto> {
        val socialUserInfo = getSocialUserInfo(request.provider, request.accessToken)
        val member = socialMemberService.createOrLinkSocialMember(
            name = socialUserInfo.name,
            email = socialUserInfo.email,
            profileImage = socialUserInfo.profileImage,
            provider = request.provider,
            providerId = socialUserInfo.id
        )
        return ResponseEntity.ok(createSocialLoginResponse(member, response))
    }

    @ApiOperation("로그인 ID 중복 체크")
    @GetMapping("/check/login-id")
    fun checkLoginId(@RequestParam loginId: String): ResponseEntity<DuplicateCheckResponse> {
        val isDuplicate = memberService.existsByLoginId(loginId)
        return ResponseEntity.ok(
            DuplicateCheckResponse(
                field = "loginId",
                value = loginId,
                isDuplicate = isDuplicate,
                message = if (isDuplicate) "이미 사용 중인 로그인 ID입니다." else "사용 가능한 로그인 ID입니다."
            )
        )
    }

    @ApiOperation("이메일 중복 체크")
    @GetMapping("/check/email")
    fun checkEmail(@RequestParam email: String): ResponseEntity<DuplicateCheckResponse> {
        val isDuplicate = memberService.findByEmail(email) != null
        return ResponseEntity.ok(
            DuplicateCheckResponse(
                field = "email",
                value = email,
                isDuplicate = isDuplicate,
                message = if (isDuplicate) "이미 사용 중인 이메일입니다." else "사용 가능한 이메일입니다."
            )
        )
    }

    @ApiOperation("닉네임 중복 체크")
    @GetMapping("/check/nickname")
    fun checkNickname(@RequestParam nickname: String): ResponseEntity<DuplicateCheckResponse> {
        val isDuplicate = memberService.findByNickname(nickname) != null
        return ResponseEntity.ok(
            DuplicateCheckResponse(
                field = "nickname",
                value = nickname,
                isDuplicate = isDuplicate,
                message = if (isDuplicate) "이미 사용 중인 닉네임입니다." else "사용 가능한 닉네임입니다."
            )
        )
    }

    @ApiOperation("내 정보 조회")
    @GetMapping("/member-info")
    fun memberInfo(@AuthenticationPrincipal member: Member) =
        ResponseEntity.ok(memberService.findActivateMemberByLoginId(member.loginId))

    @ApiOperation("비밀번호 변경")
    @PatchMapping("/password")
    fun changePassword(
        @AuthenticationPrincipal member: Member,
        @RequestBody request: MemberPasswordUpdateRequest
    ): ResponseEntity<Void> {
        memberService.changePassword(member.loginId, request)
        return ResponseEntity.ok().build()
    }

    @ApiOperation("소셜 회원 추가 정보 입력")
    @PostMapping("/social/complete-profile")
    fun completeSocialProfile(
        @AuthenticationPrincipal member: Member,
        @RequestBody request: SocialSignUpCompleteRequest
    ): ResponseEntity<Void> {
        socialMemberService.completeSocialProfile(member.loginId, request)
        return ResponseEntity.ok().build()
    }

    private fun makeToken(member: Member, response: HttpServletResponse, includeRefreshToken: Boolean = true): TokenDto {
        val accessToken = jwtService.generateToken(member, true)
        response.setHeader(HttpHeaders.AUTHORIZATION, JBConstants.BEARER + accessToken)

        if (!includeRefreshToken) {
            return TokenDto(accessToken = accessToken)
        }

        val refreshToken = jwtService.generateToken(member.setExpired(jwtTokenProperties.refreshToken), false)
        val info = memberService.findActivateMemberByLoginId(member.loginId)
        val memberId = requireNotNull(info.id) { "Member ID must not be null" }
        return TokenDto(memberId, info.nickname, accessToken, refreshToken)
    }

    private fun makeTokenWithoutRefreshToken(member: Member, response: HttpServletResponse): TokenDto {
        return makeToken(member, response, includeRefreshToken = false)
    }


    private fun createSocialLoginResponse(member: org.example.junglebook.entity.MemberEntity, response: HttpServletResponse): SocialLoginTokenDto {
        val memberAuth = Member.from(member)
        val tokenDto = makeToken(memberAuth, response)
        val loginResponse = LoginResponse.success(member)

        val memberId = requireNotNull(tokenDto.memberId) { "Member ID must not be null" }
        val nickname = requireNotNull(tokenDto.nickname) { "Nickname must not be null" }
        val refreshToken = requireNotNull(tokenDto.refreshToken) { "Refresh token must not be null" }

        return SocialLoginTokenDto(
            memberId = memberId,
            nickname = nickname,
            accessToken = tokenDto.accessToken,
            refreshToken = refreshToken,
            loginResponse = loginResponse
        )
    }

    private fun getSocialUserInfo(provider: SocialProvider, accessToken: String): SocialUserInfo {
        return when (provider) {
            SocialProvider.KAKAO -> getKakaoUserInfo(accessToken)
            SocialProvider.NAVER -> getNaverUserInfo(accessToken)
        }
    }

    private fun getKakaoUserInfo(accessToken: String): SocialUserInfo {
        return kakaoOAuthService.getUserInfo(accessToken)
    }

    private fun getNaverUserInfo(accessToken: String): SocialUserInfo {
        return naverOAuthService.getUserInfo(accessToken)
    }
}
