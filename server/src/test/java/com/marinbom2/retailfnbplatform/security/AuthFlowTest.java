package com.marinbom2.retailfnbplatform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marinbom2.retailfnbplatform.member.domain.MemberRepository;
import com.marinbom2.retailfnbplatform.member.domain.RoleType;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 실제 필터 체인으로 가입 → 로그인 → 인증 → 권한 → 로그아웃 (FR-AU-001~003, md/authentication-security.md).
 * MySQL 8.4 를 Testcontainers 로 띄운다. Docker 데몬이 없으면 건너뛴다.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "jwt.secret=test-secret-0123456789-abcdefghijklmnop",
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuthFlowTest {

    private static final String PASSWORD = "Passw0rd!";
    private static final String COOKIE = "retailfnb_at";

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FilterChainProxy filterChainProxy;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("가입 → 로그인(HttpOnly·SameSite=Lax 쿠키) → 쿠키로 내 정보")
    void signupLoginMe() throws Exception {
        String email = newEmail();

        mockMvc.perform(signup(email, PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roleType.code").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        MvcResult login = mockMvc.perform(login(email, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
                .andReturn();
        Cookie cookie = login.getResponse().getCookie(COOKIE);
        assertThat(cookie).isNotNull();

        mockMvc.perform(get("/api/v1/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    @DisplayName("가입 검증: 같은 이메일 409 ALREADY_EXIST, 이메일 형식 오류 400 INVALID_REQUEST_DATA + errors")
    void signupValidation() throws Exception {
        String email = newEmail();
        signupOk(email);

        mockMvc.perform(signup(email.toUpperCase(), PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_EXIST"));

        mockMvc.perform(signup("not-an-email", PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_DATA"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    @DisplayName("로그인 실패: 비밀번호 불일치·없는 계정 401 LOGIN_FAILED, JSON 아님 400 INVALID_REQUEST_DATA")
    void loginFailure() throws Exception {
        String email = newEmail();
        signupOk(email);

        mockMvc.perform(login(email, "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
        mockMvc.perform(login("nobody-" + email, PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.TEXT_PLAIN).content("email=a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_DATA"));
    }

    @Test
    @DisplayName("인증 없음 401 UNAUTHORIZED, 위조 쿠키 401 INVALID_ACCESS_TOKEN")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.instance").value("/api/v1/me"));

        mockMvc.perform(get("/api/v1/me").cookie(new Cookie(COOKIE, "not.a.jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("관리자 API: USER 는 403 FORBIDDEN, DB 에서 ADMIN 으로 바꾸면 같은 쿠키로 인가를 통과한다")
    void adminAuthorization() throws Exception {
        String email = newEmail();
        signupOk(email);
        Cookie cookie = loginCookie(email);

        mockMvc.perform(get("/api/v1/admin/summary").cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        transactionTemplate.executeWithoutResult(tx ->
                memberRepository.findByEmail(email).orElseThrow().changeRole(RoleType.ADMIN));

        // 권한은 토큰이 아니라 DB 값이라 바로 반영된다. 컨트롤러가 아직 없어 404 면 인가는 통과한 것
        mockMvc.perform(get("/api/v1/admin/summary").cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("로그아웃: 204 + 쿠키 삭제(Max-Age=0), 그 전에 받은 토큰은 401 INVALID_ACCESS_TOKEN")
    void logoutRevokesTokens() throws Exception {
        String email = newEmail();
        signupOk(email);
        Cookie cookie = loginCookie(email);

        mockMvc.perform(post("/api/v1/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mockMvc.perform(get("/api/v1/me").cookie(cookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("시큐리티 체인 밖: 헬스체크와 API 문서는 인증 없이 열린다 (문서에 로그인 경로 포함)")
    void outsideSecurityChain() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.summary").value("로그인"));
    }

    @Test
    @DisplayName("체인은 API 하나. JWT 필터 → 로그인 필터 → 예외 변환 → 인가 순서다")
    void filterChainOrder() {
        assertThat(filterChainProxy.getFilterChains()).hasSize(1);
        List<String> filters = filterChainProxy.getFilterChains().getFirst().getFilters().stream()
                .map(filter -> filter.getClass().getSimpleName())
                .toList();
        System.out.println("[security-filters] " + String.join(" > ", filters));

        assertThat(filters).containsSubsequence(
                "SecurityContextHolderFilter",
                "JwtAuthenticationFilter",
                "MemberAuthenticationAPIFilter",
                "AnonymousAuthenticationFilter",
                "ExceptionTranslationFilter",
                "AuthorizationFilter");
        assertThat(filters).doesNotContain("CsrfFilter", "LogoutFilter", "UsernamePasswordAuthenticationFilter",
                "BasicAuthenticationFilter", "RequestCacheAwareFilter");
    }

    private void signupOk(String email) throws Exception {
        mockMvc.perform(signup(email, PASSWORD)).andExpect(status().isCreated());
    }

    private Cookie loginCookie(String email) throws Exception {
        return mockMvc.perform(login(email, PASSWORD)).andExpect(status().isOk())
                .andReturn().getResponse().getCookie(COOKIE);
    }

    private static RequestBuilder signup(String email, String password) {
        return post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }

    private static RequestBuilder login(String email, String password) {
        return post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }

    private static String newEmail() {
        return "u" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
