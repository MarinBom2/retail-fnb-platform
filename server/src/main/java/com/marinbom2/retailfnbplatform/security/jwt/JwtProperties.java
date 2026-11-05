package com.marinbom2.retailfnbplatform.security.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * JWT·인증 쿠키 설정 ({@code jwt.*}, application.yml).
 *
 * @param secret         HS256 서명 키 (32바이트 이상). 운영은 환경변수 JWT_SECRET. 비우면 기동마다 임시 키(운영에서는 기동 실패)
 * @param accessTokenTtl 액세스 토큰 유효 기간. 쿠키 Max-Age 도 같다
 */
@ConfigurationProperties("jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("12h") Duration accessTokenTtl,
        @DefaultValue Cookie cookie
) {

    /**
     * @param secure   Secure 플래그. 운영은 반드시 true, local 은 http 라 false
     * @param sameSite Lax 면 다른 사이트에서 시작된 POST 에 쿠키가 실리지 않는다 (CSRF 방어의 핵심)
     * @param domain   비우면 응답한 호스트에만 붙는다 (host-only)
     */
    public record Cookie(
            @DefaultValue("retailfnb_at") String name,
            @DefaultValue("true") boolean secure,
            @DefaultValue("Lax") String sameSite,
            @DefaultValue("/") String path,
            String domain
    ) {
    }
}
