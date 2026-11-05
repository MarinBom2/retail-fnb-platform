package com.marinbom2.retailfnbplatform.member.api;

import com.marinbom2.retailfnbplatform.member.api.dto.MemberResponse;
import com.marinbom2.retailfnbplatform.member.api.dto.MemberSignupRequest;
import com.marinbom2.retailfnbplatform.member.application.MemberService;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.security.cookie.AccessTokenCookie;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가입·로그아웃. 로그인(POST /api/v1/auth/login)은 시큐리티 필터 MemberAuthenticationAPIFilter 가 처리한다.
 */
@Tag(name = "인증", description = "가입, 로그인, 로그아웃")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final AccessTokenCookie accessTokenCookie;

    @Operation(summary = "가입",
            description = "FR-AU-001. 이메일과 비밀번호로 회원을 만든다. 가입 뒤 로그인은 따로 한다")
    @ApiResponse(responseCode = "400", description = "INVALID_REQUEST_DATA",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "ALREADY_EXIST",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse signUp(@Valid @RequestBody MemberSignupRequest request) {
        return MemberResponse.from(memberService.signUp(request));
    }

    @Operation(summary = "로그아웃",
            description = "FR-AU-002. 이 계정으로 발급한 토큰을 모두 무효로 만들고 인증 쿠키를 지운다. 토큰이 이미 만료됐어도 쿠키는 지운다")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Member member, HttpServletResponse response) {
        if (member != null) {
            memberService.logout(member.getId());
        }
        accessTokenCookie.clear(response);
    }
}
