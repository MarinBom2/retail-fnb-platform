package com.marinbom2.retailfnbplatform.security.handler;

import com.marinbom2.retailfnbplatform.member.api.dto.MemberResponse;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.security.cookie.AccessTokenCookie;
import com.marinbom2.retailfnbplatform.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * 로그인 성공: JWT 를 HttpOnly 쿠키로 내리고, 본문은 MemberResponse(DTO 그대로, 봉투 없음).
 * 빈이 아니라 SecurityConfig 에서 직접 만든다. DB 쓰기가 없어 트랜잭션도 필요 없다 (tokenVersion 방식).
 */
@Slf4j
@RequiredArgsConstructor
public class MemberLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenCookie accessTokenCookie;
    private final JsonMapper jsonMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        Member member = (Member) authentication.getPrincipal();

        // 헤더(쿠키)는 본문을 쓰기 전에 붙여야 한다. 본문을 flush 하면 헤더는 이미 나간 뒤다
        accessTokenCookie.issue(response, jwtTokenProvider.issue(member));

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), MemberResponse.from(member));

        log.info("[auth] login memberId={}", member.getId());
    }
}
