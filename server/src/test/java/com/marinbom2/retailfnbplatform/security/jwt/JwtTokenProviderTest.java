package com.marinbom2.retailfnbplatform.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinbom2.retailfnbplatform.member.domain.Member;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-0123456789-abcdefghijklmnop";
    private static final Instant T0 = Instant.parse("2026-11-03T05:00:00Z");
    private static final Duration TTL = Duration.ofHours(12);

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, TTL, clockAt(T0));

    @Test
    @DisplayName("발급한 토큰을 읽으면 회원 ID 와 토큰 버전이 그대로 나온다")
    void roundTrip() {
        Member member = member(7L);
        member.invalidateTokens(); // 버전 1

        AccessTokenClaims claims = provider.parse(provider.issue(member));

        assertThat(claims.memberId()).isEqualTo(7L);
        assertThat(claims.tokenVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("유효 기간이 지나면 ExpiredJwtException")
    void expired() {
        String token = provider.issue(member(1L));
        JwtTokenProvider later = new JwtTokenProvider(SECRET, TTL, clockAt(T0.plus(TTL).plusSeconds(1)));

        assertThatThrownBy(() -> later.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("서명을 바꾸거나 다른 키로 만든 토큰은 JwtException")
    void tamperedOrForeign() {
        String token = provider.issue(member(1L));
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");
        JwtTokenProvider other = new JwtTokenProvider("another-secret-0123456789-abcdefghijk", TTL, clockAt(T0));

        assertThatThrownBy(() -> provider.parse(tampered)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> other.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("서명 키가 32바이트보다 짧으면 만들 때 실패한다 (기동 실패)")
    void weakSecret() {
        assertThatThrownBy(() -> new JwtTokenProvider("short-secret", TTL, clockAt(T0)))
                .isInstanceOf(WeakKeyException.class);
    }

    @Test
    @DisplayName("키를 비우면 임시 키로 동작한다 (local 전용, 운영은 JwtConfig 가 막는다)")
    void blankSecret() {
        JwtTokenProvider temp = new JwtTokenProvider("", TTL, clockAt(T0));

        assertThat(temp.parse(temp.issue(member(3L))).memberId()).isEqualTo(3L);
    }

    private static Member member(long id) {
        Member member = Member.signUp("m" + id + "@example.com", "{bcrypt}hash");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
