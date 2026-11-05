package com.marinbom2.retailfnbplatform.security.cookie;

import com.marinbom2.retailfnbplatform.security.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 액세스 토큰을 HttpOnly 쿠키로 넣고 빼는 유일한 곳 (localStorage 는 XSS 한 번이면 털린다).
 * CSRF 는 SameSite=Lax + 같은 오리진 + CORS 없음으로 막는다 (authentication-security.md 6절).
 */
@Component
@RequiredArgsConstructor
public class AccessTokenCookie {

    private final JwtProperties jwtProperties;

    /** 로그인 성공 시. Max-Age 를 토큰 유효 기간과 같게 맞춰 브라우저가 알아서 지우게 한다 */
    public void issue(HttpServletResponse response, String accessToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(accessToken, jwtProperties.accessTokenTtl()).toString());
    }

    /** 로그아웃 시. 같은 속성의 쿠키를 Max-Age=0 으로 덮어써야 브라우저가 지운다 */
    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", Duration.ZERO).toString());
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        String name = jwtProperties.cookie().name();
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst();
    }

    private ResponseCookie build(String value, Duration maxAge) {
        JwtProperties.Cookie config = jwtProperties.cookie();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(config.name(), value)
                .httpOnly(true)
                .secure(config.secure())
                .sameSite(config.sameSite())
                .path(config.path())
                .maxAge(maxAge);
        if (StringUtils.hasText(config.domain())) {
            builder.domain(config.domain());
        }
        return builder.build();
    }
}
