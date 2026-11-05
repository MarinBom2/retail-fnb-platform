package com.marinbom2.retailfnbplatform.security;

import com.marinbom2.retailfnbplatform.member.domain.MemberRepository;
import com.marinbom2.retailfnbplatform.security.cookie.AccessTokenCookie;
import com.marinbom2.retailfnbplatform.security.details.MemberDetailsService;
import com.marinbom2.retailfnbplatform.security.filter.JwtAuthenticationFilter;
import com.marinbom2.retailfnbplatform.security.filter.MemberAuthenticationAPIFilter;
import com.marinbom2.retailfnbplatform.security.handler.MemberLoginSuccessHandler;
import com.marinbom2.retailfnbplatform.security.jwt.JwtTokenProvider;
import com.marinbom2.retailfnbplatform.security.provider.MemberAuthenticationProvider;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * API 시큐리티 설정 (FR-AU-002, FR-AU-003). 구조는 authentication-security.md.
 * 직접 등록한 빈 때문에 Boot 기본 체인과 임시 계정은 만들어지지 않는다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MemberSecurityProperties.class)
public class SecurityConfig {

    /** 인증 없이 부르는 POST 경로. 로그아웃은 토큰이 만료돼도 쿠키를 지울 수 있게 연다 */
    static final String[] PERMIT_ALL_POST = {
            "/api/v1/auth/signup",
            MemberAuthenticationAPIFilter.LOGIN_PATH,
            "/api/v1/auth/logout",
    };

    /** 관리자 API. 권한 이름은 RoleType 이름 그대로 (ROLE_ 접두사 없음) */
    static final String ADMIN_PATTERN = "/api/v1/admin/**";

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                      AuthenticationManager memberAuthenticationManager,
                                                      JsonMapper jsonMapper,
                                                      JwtTokenProvider jwtTokenProvider,
                                                      AccessTokenCookie accessTokenCookie,
                                                      MemberRepository memberRepository,
                                                      SecurityErrorResponder securityErrorResponder) throws Exception {

        MemberAuthenticationAPIFilter loginFilter = new MemberAuthenticationAPIFilter(memberAuthenticationManager, jsonMapper);
        loginFilter.setAuthenticationSuccessHandler(new MemberLoginSuccessHandler(jwtTokenProvider, accessTokenCookie, jsonMapper));
        loginFilter.setAuthenticationFailureHandler(securityErrorResponder);

        http
                // 이 체인은 /api/** 만 맡는다. 화면(정적 파일), /actuator/health, /swagger-ui 는 체인을 타지 않는다
                .securityMatcher("/api/**")
                // CSRF 토큰 대신 SameSite=Lax 쿠키 + 같은 오리진(CORS 없음)으로 막는다 (authentication-security.md 6절)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)     // 로그아웃은 AuthController 가 한다
                .requestCache(AbstractHttpConfigurer::disable) // 켜 두면 401 때 요청을 세션에 저장하려 한다
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(securityErrorResponder)
                        .accessDeniedHandler(securityErrorResponder))
                // 빼면 AuthorizationFilter 가 붙지 않아 권한이 검사되지 않는다
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, PERMIT_ALL_POST).permitAll()
                        .requestMatchers(ADMIN_PATTERN).hasAnyAuthority("ADMIN", "SYSTEM")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, accessTokenCookie, memberRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(loginFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** 로그인 필터가 받는 매니저와 프로바이더가 같은 인스턴스여야 한다. 그래서 직접 만든다 */
    @Bean
    public AuthenticationManager memberAuthenticationManager(UserDetailsService memberDetailsService,
                                                             PasswordEncoder passwordEncoder,
                                                             MemberSecurityProperties properties,
                                                             Environment environment) {
        return new ProviderManager(new MemberAuthenticationProvider(
                memberDetailsService, passwordEncoder, bypassEmails(properties, environment)));
    }

    @Bean
    public UserDetailsService memberDetailsService(MemberRepository memberRepository) {
        return new MemberDetailsService(memberRepository);
    }

    /** 해시에 {bcrypt} 같은 알고리즘 표시가 붙는다. 나중에 알고리즘을 바꿔도 예전 해시를 그대로 검증할 수 있다 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** 개발용 백도어는 local 프로필에서만 연다. 다른 프로필이면 설정에 목록이 있어도 무시한다 */
    private static Set<String> bypassEmails(MemberSecurityProperties properties, Environment environment) {
        if (!environment.acceptsProfiles(Profiles.of("local"))) {
            if (!properties.loginBypass().emails().isEmpty()) {
                log.warn("[login] security.login-bypass.emails 가 있지만 local 프로필이 아니라 무시합니다");
            }
            return Set.of();
        }
        return Set.copyOf(properties.loginBypass().emails());
    }
}
