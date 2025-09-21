package org.example.junglebook.web.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletResponse
import kr.co.minust.api.exception.DefaultErrorCode
import kr.co.minust.api.exception.GlobalException
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class MemberController(
    private val jwtService: JwtService,
    private val jwtTokenProperties: JwtTokenProperties,
    private val authenticationManager: AuthenticationManager,
    private val memberService: MemberService,
    private val passwordEncoder: PasswordEncoder,
    private val socialMemberService: SocialMemberService
) {
    @Operation(
        summary = "로그인",
        description = "일반 로그인 API"
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
        summary = "소셜 로그인",
        description = "소셜 로그인 API"
    )
    @PostMapping("/api/social-login")
    fun socialLogin(@RequestBody request: SocialLoginRequest, response: HttpServletResponse): ResponseEntity<SocialLoginTokenDto> {
        return try {
            // 소셜 제공자에서 사용자 정보 조회
            val socialUserInfo = getSocialUserInfo(request.provider, request.accessToken)

            // 기존 소셜 회원 확인
            var member = socialMemberService.findBySocialLogin(request.provider, socialUserInfo.id)

            if (member != null) {
                // 기존 회원 정보 업데이트
                member = socialMemberService.updateSocialMember(
                    member = member,
                    name = socialUserInfo.name,
                    email = socialUserInfo.email,
                    profileImage = socialUserInfo.profileImage
                )
            } else {
                // 신규 소셜 회원 생성 또는 기존 일반 회원과 연동
                member = socialMemberService.createOrLinkSocialMember(
                    name = socialUserInfo.name,
                    email = socialUserInfo.email,
                    profileImage = socialUserInfo.profileImage,
                    provider = request.provider,
                    providerId = socialUserInfo.id
                )
            }

            val memberAuth = Member.from(member)
            val tokenDto = makeToken(memberAuth, response)

            // LoginResponse 사용하여 응답 생성
            val loginResponse = LoginResponse.success(member)

            ResponseEntity.ok(SocialLoginTokenDto(
                memberId = tokenDto.memberId!!,
                nickname = tokenDto.nickname!!,
                accessToken = tokenDto.accessToken,
                refreshToken = tokenDto.refreshToken!!,
                loginResponse = loginResponse
            ))
        } catch (e: Exception) {
            throw GlobalException(DefaultErrorCode.SOCIAL_LOGIN_FAILURE)
        }
    }

    @Operation(
        summary = "로그인 by RefreshToken",
        description = "리프레시 토큰으로 로그인 API"
    )
    @PostMapping("/api/sign-in-by-refresh-token")
    fun signInByRefreshToken(
        @Parameter(hidden = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION) authorization: String,
        response: HttpServletResponse
    ): ResponseEntity<TokenDto> {
        val payload = jwtService.extractRefreshToken(authorization.substringAfter(JBConstants.Companion.BEARER))
        val member = memberService.findActivateMemberByLoginId(payload.loginId)
        return ResponseEntity.ok(this.makeTokenWithoutRefreshToken(Member.from(member), response))
    }

    private fun makeToken(member: Member, response: HttpServletResponse): TokenDto {
        val accessToken = jwtService.generateToken(member, true)
        val refreshToken = jwtService.generateToken(member.setExpired(jwtTokenProperties.refreshToken), false)
        response.setHeader(HttpHeaders.AUTHORIZATION, JBConstants.Companion.BEARER + accessToken)

        // loginId로 회원 정보 조회 (일반/소셜 회원 모두 처리)
        val info = memberService.findActivateMemberByLoginId(member.loginId)
        return TokenDto(info.id!!, info.nickname, accessToken, refreshToken)
    }

    private fun makeTokenWithoutRefreshToken(member: Member, response: HttpServletResponse): TokenDto {
        val accessToken = jwtService.generateToken(member, true)
        response.setHeader(HttpHeaders.AUTHORIZATION, JBConstants.Companion.BEARER + accessToken)
        return TokenDto(accessToken = accessToken)
    }

    @Operation(
        summary = "일반 회원가입",
        description = "일반 회원가입 API"
    )
    @PostMapping("/api/signup")
    fun signUp(@RequestBody request: SignUpRequest): ResponseEntity<ApiResponse<String>> {
        return try {
            // 중복 체크
            if (memberService.findByEmail(request.email) != null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(DefaultErrorCode.EMAIL_ALREADY_EXIST.description))
            }

            if (memberService.findByNickname(request.nickname) != null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(DefaultErrorCode.NICKNAME_ALREADY_EXIST.description))
            }

            if (memberService.myInfoByLoginId(request.loginId) != null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(DefaultErrorCode.LOGIN_ID_ALREADY_EXIST.description))
            }

            // 비밀번호 인코딩
            request.encodedPassword = passwordEncoder.encode(request.password)

            memberService.signUp(request)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다."))
        } catch (e: GlobalException) {
            ResponseEntity.badRequest()
                .body(ApiResponse.error(e.errorCode?.description ?: DefaultErrorCode.SYSTEM_ERROR.description))
        } catch (e: Exception) {
            ResponseEntity.badRequest()
                .body(ApiResponse.error("회원가입 중 오류가 발생했습니다: ${e.message}"))
        }
    }

    @Operation(
        summary = "일반 회원가입 후 자동 로그인",
        description = "일반 회원가입 후 바로 로그인 처리하는 API"
    )
    @PostMapping("/api/signup-and-login")
    fun signUpAndLogin(@RequestBody request: SignUpRequest, response: HttpServletResponse): ResponseEntity<ApiResponse<TokenDto>> {
        return try {
            // 중복 체크
            if (memberService.findByEmail(request.email) != null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(DefaultErrorCode.EMAIL_ALREADY_EXIST.description))
            }

            if (memberService.findByNickname(request.nickname) != null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(DefaultErrorCode.NICKNAME_ALREADY_EXIST.description))
            }

            if (memberService.myInfoByLoginId(request.loginId) != null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(DefaultErrorCode.LOGIN_ID_ALREADY_EXIST.description))
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

            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tokenDto, "회원가입 및 로그인이 완료되었습니다."))
        } catch (e: GlobalException) {
            ResponseEntity.badRequest()
                .body(ApiResponse.error(e.errorCode?.description ?: DefaultErrorCode.SYSTEM_ERROR.description))
        } catch (e: Exception) {
            ResponseEntity.badRequest()
                .body(ApiResponse.error("회원가입 중 오류가 발생했습니다: ${e.message}"))
        }
    }

    @Operation(
        summary = "소셜 회원가입",
        description = "소셜 회원가입 API (최초 가입시 사용)"
    )
    @PostMapping("/api/social-signup")
    fun socialSignUp(@RequestBody request: SocialSignUpRequest, response: HttpServletResponse): ResponseEntity<SocialLoginTokenDto> {
        return try {
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

            // LoginResponse 사용하여 응답 생성
            val loginResponse = LoginResponse.success(member)

            ResponseEntity.ok(SocialLoginTokenDto(
                memberId = tokenDto.memberId!!,
                nickname = tokenDto.nickname!!,
                accessToken = tokenDto.accessToken,
                refreshToken = tokenDto.refreshToken!!,
                loginResponse = loginResponse
            ))
        } catch (e: GlobalException) {
            throw e // GlobalException은 그대로 전파
        } catch (e: Exception) {
            throw GlobalException(DefaultErrorCode.SOCIAL_LOGIN_FAILURE)
        }
    }

    @Operation(
        summary = "로그인 ID 중복 체크",
        description = "회원가입 시 로그인 ID 중복 확인"
    )
    @GetMapping("/api/check/login-id")
    fun checkLoginId(@RequestParam loginId: String): ResponseEntity<ApiResponse<DuplicateCheckResponse>> {
        return try {
            val isDuplicate = memberService.myInfoByLoginId(loginId) != null
            ResponseEntity.ok(
                ApiResponse.success(
                    DuplicateCheckResponse(
                        field = "loginId",
                        value = loginId,
                        isDuplicate = isDuplicate,
                        message = if (isDuplicate) "이미 사용 중인 로그인 ID입니다." else "사용 가능한 로그인 ID입니다."
                    )
                )
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest()
                .body(ApiResponse.error("로그인 ID 중복 체크 중 오류가 발생했습니다: ${e.message}"))
        }
    }

    @Operation(
        summary = "이메일 중복 체크",
        description = "회원가입 시 이메일 중복 확인"
    )
    @GetMapping("/api/check/email")
    fun checkEmail(@RequestParam email: String): ResponseEntity<ApiResponse<DuplicateCheckResponse>> {
        return try {
            val isDuplicate = memberService.findByEmail(email) != null
            ResponseEntity.ok(
                ApiResponse.success(
                    DuplicateCheckResponse(
                        field = "email",
                        value = email,
                        isDuplicate = isDuplicate,
                        message = if (isDuplicate) "이미 사용 중인 이메일입니다." else "사용 가능한 이메일입니다."
                    )
                )
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest()
                .body(ApiResponse.error("이메일 중복 체크 중 오류가 발생했습니다: ${e.message}"))
        }
    }

    @Operation(
        summary = "닉네임 중복 체크",
        description = "회원가입 시 닉네임 중복 확인"
    )
    @GetMapping("/api/check/nickname")
    fun checkNickname(@RequestParam nickname: String): ResponseEntity<ApiResponse<DuplicateCheckResponse>> {
        return try {
            val isDuplicate = memberService.findByNickname(nickname) != null
            ResponseEntity.ok(
                ApiResponse.success(
                    DuplicateCheckResponse(
                        field = "nickname",
                        value = nickname,
                        isDuplicate = isDuplicate,
                        message = if (isDuplicate) "이미 사용 중인 닉네임입니다." else "사용 가능한 닉네임입니다."
                    )
                )
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest()
                .body(ApiResponse.error("닉네임 중복 체크 중 오류가 발생했습니다: ${e.message}"))
        }
    }

    @GetMapping("/api/member-info")
    fun memberInfo(@AuthenticationPrincipal member: Member) =
        memberService.myInfoByLoginId(member.loginId)

    @Operation(
        summary = "비밀번호 변경",
        description = "비밀번호 변경 API"
    )
    @PatchMapping("/api/password")
    fun changePassword(
        @AuthenticationPrincipal member: Member,
        @RequestBody request: MemberPasswordUpdateRequest
    ) {
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
    }

    @Operation(
        summary = "소셜 회원 추가 정보 입력",
        description = "소셜 회원가입 후 추가 정보 입력 API"
    )
    @PostMapping("/api/social/complete-profile")
    fun completeSocialProfile(
        @AuthenticationPrincipal member: Member,
        @RequestBody request: SocialSignUpCompleteRequest
    ): ResponseEntity<Void> {
        socialMemberService.completeSocialProfile(member.loginId, request)
        return ResponseEntity.ok().build()
    }

    // 헬퍼 메서드
    private fun getSocialUserInfo(provider: SocialProvider, accessToken: String): SocialUserInfo {
        // TODO: 실제 소셜 제공자별 API 호출 로직 구현
        return when (provider) {
            SocialProvider.KAKAO -> getKakaoUserInfo(accessToken)
            SocialProvider.NAVER -> getNaverUserInfo(accessToken)
        }
    }

    private fun getKakaoUserInfo(accessToken: String): SocialUserInfo {
        // TODO: 카카오 API 호출 로직 구현
        throw NotImplementedError("카카오 사용자 정보 조회 구현 필요")
    }

    private fun getNaverUserInfo(accessToken: String): SocialUserInfo {
        // TODO: 네이버 API 호출 로직 구현
        throw NotImplementedError("네이버 사용자 정보 조회 구현 필요")
    }
}

// 중복 체크 응답 DTO
data class DuplicateCheckResponse(
    val field: String,        // 체크한 필드명 (loginId, email, nickname)
    val value: String,        // 체크한 값
    val isDuplicate: Boolean, // 중복 여부
    val message: String       // 사용자 메시지
)

// API 응답 래퍼 클래스
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> {
            return ApiResponse(success = true, data = data, message = message)
        }

        fun success(message: String): ApiResponse<String> {
            return ApiResponse(success = true, data = null, message = message)
        }

        fun <T> error(message: String, data: T? = null): ApiResponse<T> {
            return ApiResponse(success = false, data = data, message = message)
        }
    }
}
