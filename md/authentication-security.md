# 인증과 시큐리티 — Spring Security 7 (Spring Boot 4) 구조

> 목적: 로그인·인증·인가가 **어디서 어떤 순서로** 일어나는지, Boot 기본 설정과 우리가 등록한 빈이 어떻게 맞물리는지 정리한다.
> 기준 버전: Spring Boot 4.1.2, Spring Security 7. 구조는 계약봄(gyeyakbom) 프로젝트의 `server/security` 를 그대로 옮겼고, 소셜 로그인·이메일 인증번호는 옮기지 않았다 (10절).
> 관련 문서: [develop-principal.md](./develop-principal.md) §7·§8, [runtime-structure.md](./runtime-structure.md), [domain-design.md](./domain-design.md)

## 한눈에 보기

| 항목 | 결정 |
|---|---|
| 로그인 | `POST /api/v1/auth/login` JSON `{email, password}`. 컨트롤러가 아니라 **시큐리티 필터**(`MemberAuthenticationAPIFilter`)가 처리 |
| 인증 주체 | `Member` 엔티티가 `UserDetails` 를 구현해 그대로 principal 이 된다 |
| 토큰 | JWT(HS256), **HttpOnly 쿠키** `retailfnb_at`, 유효 12시간. claim 은 회원 ID 와 토큰 버전뿐 |
| 로그아웃 | `Member.tokenVersion` 을 1 올려 그 전에 발급한 토큰을 전부 무효화 |
| 플랫폼 권한 | 토큰이 아니라 요청마다 DB 의 현재 값. `USER`, `ADMIN`, `SYSTEM` (ROLE_ 접두사 없음) |
| 매장 권한 | `StoreStaff.role` (점주·매니저·직원). 매장 API 를 만들 때 서비스에서 확인 `[ ]` |
| 세션 | 없음 (STATELESS) |
| CSRF | 토큰 끔. SameSite=Lax 쿠키 + 같은 오리진 + CORS 없음으로 막는다 (6절) |
| 오류 | 401·403·로그인 실패 모두 ProblemDetail + `code` (`SecurityErrorResponder`) |
| 시큐리티가 맡는 경로 | `/api/**` 만. 화면, `/actuator/health`, Swagger 는 체인을 타지 않는다 |

---

## 1. Boot 3 → Boot 4 에서 달라진 점

Boot 3 은 Spring Security 6, Boot 4 는 Spring Security 7 이다. Boot 3 시절 코드를 옮길 때 걸리는 곳은 아래다.

| 항목 | Boot 3 (Security 6) | Boot 4 (Security 7) |
|---|---|---|
| 설정 체이닝 | `.and()` 로 이어 쓰기 (6.1부터 deprecated) | **제거**. 람다 DSL 만 (`http.csrf(c -> c.disable())`) |
| 인가 설정 | `authorizeRequests()` (deprecated) | **제거**. `authorizeHttpRequests()` 만 |
| 경로 매처 | `AntPathRequestMatcher`, `MvcRequestMatcher` | **제거**. `PathPatternRequestMatcher` 만 |
| JSON | `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2) | `tools.jackson.databind.json.JsonMapper` (Jackson 3) |
| `UserDetails` | 일부 default 메서드 | `isAccountNonExpired` 등 네 개가 default(true). `getAuthorities`·`getPassword`·`getUsername` 만 구현 |
| 자동 설정 위치 | `spring-boot-autoconfigure` 안 | `spring-boot-security` 모듈로 분리 |
| jjwt + Jackson | `jjwt-jackson` | `jjwt-jackson` 은 Jackson 2 를 끌고 온다. 이 프로젝트는 `jjwt-gson` (develop-principal §4) |

그대로인 것: `AbstractAuthenticationProcessingFilter`, `OncePerRequestFilter`, `ProviderManager`, `AuthenticationProvider`, `UserDetailsService`, `SecurityContextHolder`.

---

## 2. 요청이 시큐리티에 들어가는 길

```
Tomcat (서블릿 컨테이너)
  |  서블릿 필터 목록
  +- DelegatingFilterProxy  "springSecurityFilterChain"
  |     |  스프링 빈이 아닌 서블릿 필터. 이름이 같은 빈을 컨텍스트에서 찾아 위임만 한다
  |     v
  |   FilterChainProxy
  |     +- apiSecurityFilterChain  securityMatcher("/api/**")   <- 우리가 등록한 유일한 체인
  |     |     DisableEncodeUrlFilter > WebAsyncManagerIntegrationFilter > SecurityContextHolderFilter
  |     |     > HeaderWriterFilter > JwtAuthenticationFilter > MemberAuthenticationAPIFilter
  |     |     > SecurityContextHolderAwareRequestFilter > AnonymousAuthenticationFilter
  |     |     > SessionManagementFilter > ExceptionTranslationFilter > AuthorizationFilter
  |     +- (맞는 체인이 없으면 시큐리티 필터를 하나도 거치지 않고 통과)
  v
DispatcherServlet -> 컨트롤러 -> @RestControllerAdvice
```

1. **서블릿 컨테이너에 등록되는 것은 `DelegatingFilterProxy` 하나다.** 실제 일은 컨텍스트 안의 `FilterChainProxy` 빈에게 넘긴다.
2. **`FilterChainProxy` 가 요청마다 체인을 고른다.** 우리 체인은 `/api/**` 만 맡으므로 화면, `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs` 는 시큐리티를 전혀 거치지 않는다.
3. **필터 순서는 테스트로 고정한다.** `AuthFlowTest.filterChainOrder` 가 실행 중인 `FilterChainProxy` 에서 꺼내 확인한다.

꺼 둔 필터: `CsrfFilter`(6절), `LogoutFilter`(로그아웃은 컨트롤러), `UsernamePasswordAuthenticationFilter`(폼 로그인), `BasicAuthenticationFilter`, `RequestCacheAwareFilter`(켜 두면 401 때 요청을 세션에 저장하려 한다).

---

## 3. 기본 빈과 우리가 등록한 빈

| Boot 기본 | 만드는 조건 | 우리가 등록한 빈 | 결과 |
|---|---|---|---|
| `defaultSecurityFilterChain` (모든 요청 인증 + 폼 로그인 + HTTP Basic) | `SecurityFilterChain` 빈이 없을 때 | `apiSecurityFilterChain` | 물러남. 체인이 우리 것 하나뿐 |
| `InMemoryUserDetailsManager` + 임시 비밀번호 로그 | `AuthenticationManager`·`UserDetailsService` 등의 빈이 없을 때 | `memberAuthenticationManager`, `memberDetailsService` | 물러남 |
| `PasswordEncoder` | 없음 | `passwordEncoder` (DelegatingPasswordEncoder, 해시 앞에 `{bcrypt}`) | 가입·로그인이 같은 인코더 |
| JSON 매퍼 | `JacksonAutoConfiguration` | (Boot 것을 주입받아 씀) | 로그인 필터·오류 응답이 컨트롤러와 같은 설정 |

- **필터를 `@Component` 로 만들지 않는다.** Boot 는 `Filter` 타입 빈을 서블릿 필터로도 자동 등록한다. 빈이면 체인 안에서 한 번, 체인 밖에서 한 번 더 돈다. 그래서 `SecurityConfig` 에서 `new` 로 만들어 체인에만 넣는다.

---

## 4. 로그인: 인증 매니저와 프로바이더

```
POST /api/v1/auth/login {"email","password"}
  |
MemberAuthenticationAPIFilter.attemptAuthentication
  |  Content-Type 확인, JSON 읽기, 이메일 정규화(Member.normalizeEmail)
  |  -> MemberAuthenticationToken.unauthenticated(email, password)
  v
AuthenticationManager = ProviderManager      (SecurityConfig.memberAuthenticationManager 빈)
  v
MemberAuthenticationProvider.authenticate
  |  MemberDetailsService.loadUserByUsername(email) -> Member (UserDetails)
  |  비밀번호 비교 (local 백도어 목록이면 건너뜀, 8절)
  v
성공: MemberLoginSuccessHandler          실패: SecurityErrorResponder
  JWT 발급 -> HttpOnly 쿠키                  JSON 형식 오류 -> 400 INVALID_REQUEST_DATA
  본문 MemberResponse (200)                  계정·비밀번호 불일치 -> 401 LOGIN_FAILED
```

- **매니저를 직접 빈으로 만드는 이유**: 로그인 필터가 쓰는 매니저와 그 안의 프로바이더가 정확히 우리 것이어야 한다.
- **토큰 타입을 따로 두는 이유**: `MemberAuthenticationToken` 을 쓰면 `ProviderManager` 가 `supports()` 로 이 토큰을 `MemberAuthenticationProvider` 에만 보낸다. 다른 인증 방식(소셜 로그인, 매장 POS 단말 PIN)을 더할 때 토큰 타입과 프로바이더를 하나씩 더하면 된다.
- **계정 존재 여부를 숨긴다**: 없는 이메일과 비밀번호 불일치를 같은 `BadCredentialsException`(→ 401 `LOGIN_FAILED`)으로 낸다. 없는 이메일일 때도 해시 비교를 한 번 해서 응답 시간으로 구분되지 않게 한다.

---

## 5. 요청마다 인증: JWT 필터와 토큰 무효화

```
JwtAuthenticationFilter (OncePerRequestFilter)
  토큰 꺼내기: 쿠키 retailfnb_at 우선, 없으면 Authorization: Bearer
    없음        -> 아무것도 안 하고 다음 필터 (보호 경로면 뒤에서 401)
    있음        -> JwtTokenProvider.parse: 서명·만료 검증
                   실패 -> 요청 속성에 INVALID_ACCESS_TOKEN 남기고 다음 필터
                   성공 -> DB 에서 회원 조회 + tokenVersion 비교
                           불일치·없음 -> INVALID_ACCESS_TOKEN 남기고 다음 필터
                           일치 -> SecurityContext 에 Member 를 principal 로 넣음
ExceptionTranslationFilter / AuthorizationFilter
  인증 없음 + 보호 경로 -> SecurityErrorResponder.commence -> 401 (남긴 이유가 있으면 그 code)
  권한 부족            -> SecurityErrorResponder.handle   -> 403 FORBIDDEN
```

| 결정 | 이유 |
|---|---|
| 필터는 응답을 쓰지 않는다 | 응답을 쓰는 곳을 `SecurityErrorResponder` 하나로 둔다 |
| 토큰에는 회원 ID 와 토큰 버전만 | 서명만 하고 암호화하지 않으므로 노출돼도 되는 값만 |
| 권한은 요청마다 DB 에서 | 권한을 바꾸면 같은 토큰으로 바로 반영된다 (`AuthFlowTest.adminAuthorization`). 대신 요청마다 회원을 기본키로 한 번 읽는다. 늘면 Redis 캐시 `[ ]` |
| 로그아웃은 tokenVersion + 1 | 토큰 테이블 없이 회원의 버전 숫자 하나로 폐기한다. 여러 기기 동시 로그인이 되고, 로그아웃하면 모든 기기에서 풀린다 |
| 운영에서 서명 키 필수 | `prod` 프로필에서 `jwt.secret` 이 비면 기동을 멈춘다 (`JwtConfig`). 임시 키로 뜨면 인스턴스마다 키가 달라 로그인이 풀린다 |

principal 로 들어가는 `Member` 는 트랜잭션 밖에서 읽은 준영속 객체다. 값을 바꿀 때는 ID 로 다시 읽어 서비스의 트랜잭션 안에서 바꾼다. 컨트롤러에서는 `@AuthenticationPrincipal Member member` 로 받고, 컨트롤러 밖에서는 `AccessUserInfo.getLoginMember()` 를 쓴다.

---

## 6. CSRF와 쿠키

| 장치 | 효과 |
|---|---|
| 인증 쿠키 `SameSite=Lax` | 다른 사이트에서 시작된 POST·PATCH·DELETE 요청에는 브라우저가 쿠키를 싣지 않는다 |
| 상태를 바꾸는 GET 을 만들지 않는다 | Lax 는 다른 사이트에서 링크로 들어오는 GET 에는 쿠키를 싣는다. 그래서 GET 은 조회만 한다 |
| 같은 오리진 + CORS 설정 없음 | 화면과 API 가 한 오리진이라 CORS 를 열지 않는다 ([runtime-structure.md](./runtime-structure.md) 4절) |

- 쿠키 속성: `HttpOnly`, `Secure`(운영 true, local 은 http 라 false), `Path=/`, `Max-Age`=토큰 유효 기간, 도메인 지정 없음(host-only). 쿠키를 넣고 빼는 곳은 `AccessTokenCookie` 하나다.
- 프론트는 토큰을 만지지 않는다. `localStorage` 에 두지 않는다 (XSS 한 번이면 털린다). `fetch` 에 `credentials: 'include'` 만 붙인다 (`shared/core/apiClient.ts`).

---

## 7. 오류 응답

| 자리 | 상황 | 응답 |
|---|---|---|
| `AuthenticationEntryPoint` | 인증 없이 보호 경로 | 401 `UNAUTHORIZED`, JWT 필터가 남긴 이유가 있으면 401 `INVALID_ACCESS_TOKEN` |
| `AccessDeniedHandler` | 권한 부족 | 403 `FORBIDDEN` (401 이면 클라이언트가 로그아웃시키므로 반드시 403) |
| `AuthenticationFailureHandler` | 로그인 실패 | 400 `INVALID_REQUEST_DATA`(형식) / 401 `LOGIN_FAILED`(계정·비밀번호) |

성공 응답(로그인 본문)은 `MemberLoginSuccessHandler` 가 쓴다. 두 곳 모두 Boot 의 `JsonMapper` 를 주입받아 쓴다.

---

## 8. 개발용 백도어

| 기능 | 설정 (기본값) | 켜지는 조건 |
|---|---|---|
| 비밀번호 없이 로그인 | `security.login-bypass.emails` (빈 목록) | **local 프로필**이면서 목록에 이메일이 있을 때. 다른 프로필이면 목록이 있어도 무시하고 경고 로그 |

- 백도어가 켜지면 기동 로그와 로그인마다 경고를 남긴다.
- 개인 이메일은 커밋하는 `application-local.yml` 이 아니라 `secrets/application-secret.yml` 에 넣는다.

---

## 9. 클래스 목록

| 클래스 | 모듈·패키지 | 역할 |
|---|---|---|
| `Member` | core `member.domain` | 회원 엔티티 + `UserDetails`. 이메일 정규화, 토큰 버전 |
| `MemberRepository` | core `member.domain` | `findByEmail`, `existsByEmail` |
| `SecurityConfig` | server `security` | 필터 체인, 인증 매니저, `UserDetailsService`, `PasswordEncoder` |
| `MemberAuthenticationAPIFilter` | server `security.filter` | JSON 로그인 |
| `MemberAuthenticationToken` | server `security.token` | 로그인 요청 토큰 |
| `MemberAuthenticationProvider` | server `security.provider` | 비밀번호 검증, 백도어 |
| `MemberDetailsService` | server `security.details` | 이메일로 `Member` 조회 |
| `MemberLoginSuccessHandler` | server `security.handler` | JWT 쿠키 + 회원 정보 본문 |
| `JwtAuthenticationFilter` | server `security.filter` | 요청마다 토큰 확인 |
| `JwtTokenProvider`, `JwtProperties`, `JwtConfig` | server `security.jwt` | 발급·검증, 설정, 운영 키 확인 |
| `AccessTokenCookie` | server `security.cookie` | 쿠키 발급·삭제·읽기 |
| `SecurityErrorResponder` | server `security` | 401·403·로그인 실패 ProblemDetail |
| `MemberSecurityProperties` | server `security` | 백도어 설정 |
| `AccessUserInfo` | server `security` | 컨트롤러 밖에서 로그인 회원 꺼내기 |
| `AuthController`, `MeController`, `MemberService` | server `member.api`, `member.application` | 가입, 로그아웃, 내 정보 |

API:

| 메서드 | 경로 | 인증 | FR |
|---|---|---|---|
| POST | `/api/v1/auth/signup` | 없음 | FR-AU-001 |
| POST | `/api/v1/auth/login` | 없음 (필터) | FR-AU-002 |
| POST | `/api/v1/auth/logout` | 없음 (있으면 토큰 무효화, 항상 쿠키 삭제) | FR-AU-002 |
| GET | `/api/v1/me` | 필요 | FR-AU-002 |
| * | `/api/v1/admin/**` | `ADMIN`, `SYSTEM` | FR-AU-003 |

---

## 10. 계약봄에서 옮기면서 달라진 점

| 항목 | 계약봄 | 이 프로젝트 | 이유 |
|---|---|---|---|
| 쿠키 이름 | `gyeyakbom_at` | `retailfnb_at` | 같은 브라우저에서 두 서비스를 열어도 섞이지 않게 |
| 가입 | 이메일 인증번호 확인 뒤 가입 + 약관·개인정보·해외 처리 동의 | 이메일·비밀번호만 | 인증번호 메일·동의 항목은 이 서비스 요구사항이 정해지면 붙인다 `[ ]` |
| 소셜 로그인 | 카카오·네이버 (`security.oauth2`, 체인 2개) | 없음 (체인 1개) | 요구사항 없음. 붙이면 계약봄 `SocialLoginConfig` 를 옮긴다 |
| 관리자 경로 | `/api/v1/ops/**` | `/api/v1/admin/**` | 운영자 화면이 아니라 플랫폼 관리(브랜드 등록 등) |
| 내 권한 바꾸기 | `PATCH /api/v1/me/role` (local) | 없음 | 쓰는 화면이 없다 (ISP). 테스트는 DB 에서 직접 바꾼다 |
| 회원 필드 | CI, 동의 시각 세 가지 | 없음 | 이 서비스에서 쓰지 않는다 |
| 매장 단위 권한 | 없음 | `StoreStaff.role` | 매장 관리 도메인 ([domain-design.md](./domain-design.md) §5.1) |

---

## 11. 테스트

| 테스트 | 확인하는 것 |
|---|---|
| `MemberTest` (core) | 이메일 정규화, 기본 권한, 토큰 버전, `UserDetails` 구현 |
| `JwtTokenProviderTest` | 발급·검증 왕복, 만료, 위조·다른 키, 짧은 키 기동 실패, 빈 키 임시 동작 |
| `MemberAuthenticationProviderTest` | 성공, 계정 없음과 비밀번호 불일치가 같은 예외, 백도어, `supports` |
| `SecurityErrorResponderTest` | 401·403 ProblemDetail, JWT 필터가 남긴 이유 반영 |
| `AuthFlowTest` (MySQL Testcontainers, Docker 없으면 건너뜀) | 가입 → 로그인 쿠키 → 내 정보, 가입 검증(409·400), 로그인 실패(401·400), 무인증·위조 401, 관리자 API 403 → 권한 변경 후 통과, 로그아웃 후 토큰 거절, 체인 밖 경로, **실제 필터 순서** |

## 12. 정하지 않은 것

- [ ] 토큰 갱신(refresh). 지금은 12시간 토큰 하나. 만료되면 다시 로그인
- [ ] 로그인 실패 횟수 제한(계정 잠금·요청 속도 제한)
- [ ] 비밀번호 재설정, 탈퇴
- [ ] 매장 POS 단말 로그인 방식 (직원 PIN 등)
- [ ] 매장 API 인가: `StoreStaff` 확인을 서비스에서 할지, 메서드 시큐리티(`@PreAuthorize`)로 할지
