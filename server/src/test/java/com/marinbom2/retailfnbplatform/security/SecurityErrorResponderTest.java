package com.marinbom2.retailfnbplatform.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 시큐리티 단계 401/403 이 컨트롤러 오류와 같은 ProblemDetail 형식인지.
 * Boot 가 만든 JsonMapper 를 그대로 써서, 확장 필드(code)가 최상위로 펼쳐지는 것까지 확인한다.
 */
class SecurityErrorResponderTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));

    @Test
    @DisplayName("인증 없음: 401 UNAUTHORIZED, problem+json, instance 는 요청 경로")
    void unauthorized() {
        contextRunner.run(context -> {
            JsonMapper jsonMapper = context.getBean(JsonMapper.class);
            MockHttpServletResponse response = new MockHttpServletResponse();

            new SecurityErrorResponder(jsonMapper)
                    .commence(request("/api/v1/orders/1"), response, new BadCredentialsException("x"));

            JsonNode body = jsonMapper.readTree(response.getContentAsString());
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            assertThat(body.get("code").asString()).isEqualTo("UNAUTHORIZED");
            assertThat(body.get("detail").asString()).isEqualTo(ErrorCode.UNAUTHORIZED.getDescription());
            assertThat(body.get("instance").asString()).isEqualTo("/api/v1/orders/1");
            assertThat(body.has("properties")).isFalse();
        });
    }

    @Test
    @DisplayName("JWT 필터가 남긴 실패 이유가 있으면 그 code 로 응답한다")
    void reasonFromJwtFilter() {
        contextRunner.run(context -> {
            JsonMapper jsonMapper = context.getBean(JsonMapper.class);
            MockHttpServletRequest request = request("/api/v1/me");
            request.setAttribute(SecurityErrorResponder.ERROR_CODE_ATTRIBUTE, ErrorCode.INVALID_ACCESS_TOKEN);
            MockHttpServletResponse response = new MockHttpServletResponse();

            new SecurityErrorResponder(jsonMapper).commence(request, response, new BadCredentialsException("x"));

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(jsonMapper.readTree(response.getContentAsString()).get("code").asString())
                    .isEqualTo("INVALID_ACCESS_TOKEN");
        });
    }

    @Test
    @DisplayName("권한 부족: 401 이 아니라 403 FORBIDDEN (클라이언트가 로그아웃하지 않게)")
    void forbidden() {
        contextRunner.run(context -> {
            JsonMapper jsonMapper = context.getBean(JsonMapper.class);
            MockHttpServletResponse response = new MockHttpServletResponse();

            new SecurityErrorResponder(jsonMapper)
                    .handle(request("/api/v1/admin/summary"), response, new AccessDeniedException("x"));

            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(jsonMapper.readTree(response.getContentAsString()).get("code").asString())
                    .isEqualTo("FORBIDDEN");
        });
    }

    private static MockHttpServletRequest request(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }
}
