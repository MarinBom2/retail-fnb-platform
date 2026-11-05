package com.marinbom2.retailfnbplatform.security.filter;

import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.member.domain.MemberRepository;
import com.marinbom2.retailfnbplatform.security.SecurityErrorResponder;
import com.marinbom2.retailfnbplatform.security.cookie.AccessTokenCookie;
import com.marinbom2.retailfnbplatform.security.jwt.AccessTokenClaims;
import com.marinbom2.retailfnbplatform.security.jwt.JwtTokenProvider;
import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 토큰(쿠키, 없으면 Bearer)을 확인해 SecurityContext 에 회원을 넣는다 (authentication-security.md 5절).
 * 응답은 쓰지 않고 무효면 이유만 남긴다. 빈으로 만들면 서블릿 필터로 한 번 더 돌아서 빈이 아니다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenCookie accessTokenCookie;
    private final MemberRepository memberRepository;

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
    private final WebAuthenticationDetailsSource detailsSource = new WebAuthenticationDetailsSource();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        extractAccessToken(request).ifPresent(token -> authenticate(token, request));
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        AccessTokenClaims claims;
        try {
            claims = jwtTokenProvider.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            reject(request, "invalid or expired: " + e.getClass().getSimpleName());
            return;
        }

        Optional<Member> member = memberRepository.findById(claims.memberId())
                .filter(m -> m.isCurrentTokenVersion(claims.tokenVersion()));
        if (member.isEmpty()) {
            reject(request, "revoked or unknown member: " + claims.memberId());
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(member.get(), null, member.get().getAuthorities());
        authentication.setDetails(detailsSource.buildDetails(request));

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
    }

    private void reject(HttpServletRequest request, String reason) {
        log.debug("[jwt] {} {}", request.getRequestURI(), reason);
        request.setAttribute(SecurityErrorResponder.ERROR_CODE_ATTRIBUTE, ErrorCode.INVALID_ACCESS_TOKEN);
    }

    private Optional<String> extractAccessToken(HttpServletRequest request) {
        return accessTokenCookie.read(request).or(() -> extractBearerToken(request));
    }

    /** 비브라우저 호출(Swagger 직접 입력, 스크립트)용 */
    private static Optional<String> extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(header.substring(BEARER_PREFIX.length()));
    }
}
