package com.marinbom2.retailfnbplatform.security.jwt;

import com.marinbom2.retailfnbplatform.member.domain.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;

/**
 * 액세스 토큰 발급·검증 (FR-AU-002). 암호화하지 않으므로 claim 은 회원 ID(sub)와 토큰 버전(ver)만.
 */
@Slf4j
public class JwtTokenProvider {

    static final String VERSION_CLAIM = "ver";

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;
    private final Clock clock;

    public JwtTokenProvider(String secret, Duration accessTokenTtl, Clock clock) {
        this.signingKey = resolveSigningKey(secret);
        this.accessTokenTtl = accessTokenTtl;
        this.clock = clock;
        log.info("[jwt] access token ttl={}", accessTokenTtl);
    }

    private static SecretKey resolveSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("[jwt] jwt.secret 이 비어 있어 임시 키를 만들었습니다. 재기동하면 발급된 토큰이 전부 무효가 됩니다 (local 전용)");
            return Jwts.SIG.HS256.key().build();
        }
        // 32바이트 미만이면 WeakKeyException 으로 기동이 멈춘다
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(Member member) {
        Instant now = clock.instant();
        return Jwts.builder()
                .header().type("JWT").and()
                .subject(String.valueOf(member.getId()))
                .claim(VERSION_CLAIM, member.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * 서명과 만료를 함께 검증한다.
     *
     * @throws JwtException 서명 불일치, 만료(ExpiredJwtException), 형식 오류. 호출부는 구분하지 않고 "무효 토큰"으로 다룬다
     */
    public AccessTokenClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        try {
            long memberId = Long.parseLong(claims.getSubject());
            Number version = claims.get(VERSION_CLAIM, Number.class);
            if (version == null) {
                throw new JwtException("ver claim 이 없습니다");
            }
            return new AccessTokenClaims(memberId, version.intValue());
        } catch (NumberFormatException e) {
            throw new JwtException("sub 가 회원 ID 형식이 아닙니다", e);
        }
    }
}
