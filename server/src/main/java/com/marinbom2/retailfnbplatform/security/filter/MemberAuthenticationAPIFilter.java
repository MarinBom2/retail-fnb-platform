package com.marinbom2.retailfnbplatform.security.filter;

import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.security.token.MemberAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON 로그인 POST /api/v1/auth/login {email, password} (FR-AU-002).
 * 필터라 Swagger 에는 OpenApiConfig 가 직접 적는다.
 */
public class MemberAuthenticationAPIFilter extends AbstractAuthenticationProcessingFilter {

    public static final String LOGIN_PATH = "/api/v1/auth/login";

    private final JsonMapper jsonMapper;

    public MemberAuthenticationAPIFilter(AuthenticationManager authenticationManager, JsonMapper jsonMapper) {
        super(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, LOGIN_PATH));
        setAuthenticationManager(authenticationManager);
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException {
        String contentType = request.getContentType();
        if (contentType == null || !MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(contentType))) {
            throw new AuthenticationServiceException("Content-Type 은 application/json 이어야 합니다");
        }

        JsonNode body;
        try (InputStream in = request.getInputStream()) {
            body = jsonMapper.readTree(in);
        } catch (JacksonException e) {
            throw new AuthenticationServiceException("로그인 요청 JSON 형식이 아닙니다", e);
        }

        String email = Member.normalizeEmail(body.path("email").asString(""));
        String password = body.path("password").asString("");
        if (email.isEmpty() || password.isEmpty()) {
            throw new AuthenticationServiceException("이메일과 비밀번호가 필요합니다");
        }

        MemberAuthenticationToken token = MemberAuthenticationToken.unauthenticated(email, password);
        token.setDetails(authenticationDetailsSource.buildDetails(request));
        return getAuthenticationManager().authenticate(token);
    }
}
