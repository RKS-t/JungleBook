package org.example.junglebook.web.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletResponse
import org.example.junglebook.exception.DefaultErrorCode
import org.example.junglebook.exception.GlobalException
import lombok.RequiredArgsConstructor
import org.example.junglebook.constant.JBConstants
import org.example.junglebook.enums.MemberType
import org.example.junglebook.enums.SocialProvider
import org.example.junglebook.web.dto.LoginRequest
import org.example.junglebook.web.dto.TokenDto
import org.example.junglebook.model.Member
import org.example.junglebook.properties.JwtTokenProperties
import org.example.junglebook.service.JwtService
import org.example.junglebook.service.MemberService
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
import org.springframework.security.crypto.password.PasswordEncoder
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
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
class MemberController(
    private val jwtService: JwtService,
    private val jwtTokenProperties: JwtTokenProperties,
    private val authenticationManager: AuthenticationManager,
    private val memberService: MemberService,
    private val passwordEncoder: PasswordEncoder,
    private val socialMemberService: SocialMemberService
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
        // 소셜 제공자에서 사용자 정보 조회
        val socialUserInfo = getSocialUserInfo(request.provider, request.accessToken)

        // 기존 소셜 회원 확인
        var member = socialMemberService.findBySocialLogin(request.provider, socialUserInfo.id)

        member = if (member != null) {
            // 기존 회원 정보 업데이트
            socialMemberService.updateSocialMember(
                member = member,
                name = socialUserInfo.name,
                email = socialUserInfo.email,
                profileImage = socialUserInfo.profileImage
            )
        } else {
            // 신규 소셜 회원 생성 또는 기존 일반 회원과 연동
            socialMemberService.createOrLinkSocialMember(
                name = socialUserInfo.name,
                email = socialUserInfo.email,
                profileImage = socialUserInfo.profileImage,
                provider = request.provider,
                providerId = socialUserInfo.id
            )
        }

        val memberAuth = Member.from(member)
        val tokenDto = makeToken(memberAuth, response)
        val loginResponse = LoginResponse.success(member)

        return ResponseEntity.ok(SocialLoginTokenDto(
            memberId = tokenDto.memberId!!,
            nickname = tokenDto.nickname!!,
            accessToken = tokenDto.accessToken,
            refreshToken = tokenDto.refreshToken!!,
            loginResponse = loginResponse
        ))
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
        // 중복 체크
        if (memberService.findByEmail(request.email) != null) {
            throw GlobalException(DefaultErrorCode.EMAIL_ALREADY_EXIST)
        }

        if (memberService.findByNickname(request.nickname) != null) {
            throw GlobalException(DefaultErrorCode.NICKNAME_ALREADY_EXIST)
        }

        if (memberService.myInfoByLoginId(request.loginId) != null) {
            throw GlobalException(DefaultErrorCode.LOGIN_ID_ALREADY_EXIST)
        }

        // 비밀번호 인코딩
        request.encodedPassword = passwordEncoder.encode(request.password)

        memberService.signUp(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @ApiOperation("일반 회원가입 후 자동 로그인")
    @PostMapping("/signup-and-login")
    fun signUpAndLogin(@RequestBody request: SignUpRequest, response: HttpServletResponse): ResponseEntity<TokenDto> {
        // 중복 체크
        if (memberService.findByEmail(request.email) != null) {
            throw GlobalException(DefaultErrorCode.EMAIL_ALREADY_EXIST)
        }

        if (memberService.findByNickname(request.nickname) != null) {
            throw GlobalException(DefaultErrorCode.NICKNAME_ALREADY_EXIST)
        }

        if (memberService.myInfoByLoginId(request.loginId) != null) {
            throw GlobalException(DefaultErrorCode.LOGIN_ID_ALREADY_EXIST)
        }

        // 비밀번호 인코딩
        request.encodedPassword = passwordEncoder.encode(request.password)

        // 회원가입 처리
        memberService.signUp(request)

        // 자동 로그인 처리
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
        // 소셜 제공자에서 사용자 정보 조회
        val socialUserInfo = getSocialUserInfo(request.provider, request.accessToken)

        // 신규 소셜 회원 생성 (중복 체크 포함)
        val member = socialMemberService.createOrLinkSocialMember(
            name = socialUserInfo.name,
            email = socialUserInfo.email,
            profileImage = socialUserInfo.profileImage,
            provider = request.provider,
            providerId = socialUserInfo.id
        )

        val memberAuth = Member.from(member)
        val tokenDto = makeToken(memberAuth, response)
        val loginResponse = LoginResponse.success(member)

        return ResponseEntity.ok(SocialLoginTokenDto(
            memberId = tokenDto.memberId!!,
            nickname = tokenDto.nickname!!,
            accessToken = tokenDto.accessToken,
            refreshToken = tokenDto.refreshToken!!,
            loginResponse = loginResponse
        ))
    }

    @ApiOperation("로그인 ID 중복 체크")
    @GetMapping("/check/login-id")
    fun checkLoginId(@RequestParam loginId: String): ResponseEntity<DuplicateCheckResponse> {
        val isDuplicate = memberService.myInfoByLoginId(loginId) != null
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
        ResponseEntity.ok(memberService.myInfoByLoginId(member.loginId))

    @ApiOperation("비밀번호 변경")
    @PatchMapping("/password")
    fun changePassword(
        @AuthenticationPrincipal member: Member,
        @RequestBody request: MemberPasswordUpdateRequest
    ): ResponseEntity<Void> {
        if (!request.isCorrect()) {
            throw GlobalException(DefaultErrorCode.PASSWORD_MISMATCH)
        }

        val memberEntity = memberService.findActivateMemberByLoginId(member.loginId)

        // 소셜 회원은 비밀번호 변경 불가
        if (memberEntity.memberType == MemberType.SOCIAL) {
            throw GlobalException(DefaultErrorCode.SOCIAL_MEMBER_PASSWORD_CHANGE_DENIED)
        }

        if (!passwordEncoder.matches(request.password, memberEntity.password)) {
            throw GlobalException(DefaultErrorCode.CURRENT_PASSWORD_INCORRECT)
        }

        val encodedPassword = passwordEncoder.encode(request.newPassword1)
        memberService.passwordUpdate(memberEntity, encodedPassword)

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

    // ===== Private Helper Methods =====

    private fun makeToken(member: Member, response: HttpServletResponse): TokenDto {
        val accessToken = jwtService.generateToken(member, true)
        val refreshToken = jwtService.generateToken(member.setExpired(jwtTokenProperties.refreshToken), false)
        response.setHeader(HttpHeaders.AUTHORIZATION, JBConstants.BEARER + accessToken)

        val info = memberService.findActivateMemberByLoginId(member.loginId)
        return TokenDto(info.id!!, info.nickname, accessToken, refreshToken)
    }

    private fun makeTokenWithoutRefreshToken(member: Member, response: HttpServletResponse): TokenDto {
        val accessToken = jwtService.generateToken(member, true)
        response.setHeader(HttpHeaders.AUTHORIZATION, JBConstants.BEARER + accessToken)
        return TokenDto(accessToken = accessToken)
    }

    private fun getSocialUserInfo(provider: SocialProvider, accessToken: String): SocialUserInfo {
        return when (provider) {
            SocialProvider.KAKAO -> getKakaoUserInfo(accessToken)
            SocialProvider.NAVER -> getNaverUserInfo(accessToken)
        }
    }

    private fun getKakaoUserInfo(accessToken: String): SocialUserInfo {
        // TODO: 카카오 API 호출 로직 구현
        // 실제 구현 시 RestTemplate 또는 WebClient 사용
        return SocialUserInfo(
            id = "kakao_${System.currentTimeMillis()}",
            name = "카카오사용자",
            email = "kakao@example.com",
            nickname = "카카오사용자",
            profileImage = "https://via.placeholder.com/100"
        )
    }

    private fun getNaverUserInfo(accessToken: String): SocialUserInfo {
        // TODO: 네이버 API 호출 로직 구현
        // 실제 구현 시 RestTemplate 또는 WebClient 사용
        return SocialUserInfo(
            id = "naver_${System.currentTimeMillis()}",
            name = "네이버사용자",
            email = "naver@example.com",
            nickname = "네이버사용자",
            profileImage = "https://via.placeholder.com/100"
        )
    }
}
