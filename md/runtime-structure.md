# 실행·빌드 구조 — 한 오리진 원칙

> 목적: 화면(React)과 API(Spring)를 **어떻게 띄우고, 어떻게 묶고, 어떻게 배포하는지**를 한곳에 정리한다. 로컬 개발은 편하게(HMR), 운영은 단순하게(jar 하나), 그리고 둘의 동작 조건은 같게 만드는 것이 목표다.
> 코드 안의 구조(도메인, 포트, 모듈)가 아니라 **실행·배포 아키텍처**, 즉 프로세스와 포트와 빌드 산출물이 어떻게 연결되는지에 대한 결정이다.
> 관련 문서: [develop-principal.md](./develop-principal.md) §5, [authentication-security.md](./authentication-security.md) 6절

## 한눈에 보기

| 패턴 | 이 프로젝트에서 | 얻는 것 |
|---|---|---|
| **같은 오리진 서빙** (Same-Origin Serving) | 브라우저는 화면과 API 를 늘 한 주소(스킴+호스트+포트)에서 받는다 | CORS 설정이 필요 없다. HttpOnly 인증 쿠키가 그대로 동작한다 |
| **단일 배포 단위** (SPA 를 백엔드 jar 에 번들) | 빌드된 화면(client/dist)이 server.jar 의 `static/` 안에 들어간다 | 배포물이 jar 하나. 화면과 API 버전이 어긋날 일이 없다 |
| **개발 서버 프록시** (Dev Server Proxy) | 로컬에서는 Vite(9191)가 화면을 주고, `/api` 만 서버(9190)로 넘긴다 | HMR 로 즉시 확인하면서도 브라우저 입장에서는 운영과 같은 한 오리진 |

핵심은 세 실행 방식 모두 **브라우저가 보기에 오리진이 하나**라는 점이다. 그래서 로컬에서 되는 것은 운영에서도 된다.

---

## 1. 세 가지 실행 방식

| 방식 | 언제 | 브라우저 주소 | 화면을 주는 곳 | API 를 주는 곳 |
|---|---|---|---|---|
| A. 로컬 개발 (기본) | 화면·API 개발할 때 | `localhost:9191` | Vite dev 서버 (HMR) | Vite 가 9190 으로 프록시 |
| B. 로컬 통합 확인 | 배포 전에 jar 처럼 확인할 때 | `localhost:9190` | Spring (`static/`) | Spring |
| C. 운영 | 실제 서비스 | `[ ]` (도메인 미정) | Spring (jar 안 `static/`) | Spring |

### A. 로컬 개발

```
브라우저 --> localhost:9191 (Vite dev 서버)
               |- /, /assets, /src ...   Vite 가 직접 응답 (HMR)
               +- /api/**   ------------> localhost:9190 (Spring, local 프로필)
```

```bash
# 터미널 0: DB·Redis
docker compose up -d
```

```bash
# 터미널 1: 서버 (npm 빌드가 돌지 않는다)
./gradlew :server:bootRun
```

```bash
# 터미널 2: 화면
cd client && npm install && npm run dev
```

Windows PowerShell 은 `.\gradlew.bat :server:bootRun`. IntelliJ 에서 서버를 실행해도 같다 (local 프로필은 `bootRun` 이 기본으로 켠다. IDE 실행이면 실행 설정에 `SPRING_PROFILES_ACTIVE=local`).

### B. 로컬 통합 확인

```
브라우저 --> localhost:9190 (Spring)
               |- /, /assets/**   static/ (방금 빌드한 client/dist)
               +- /api/**         컨트롤러
```

```bash
./gradlew :server:bootRun -PwithClient
```

`-PwithClient` 를 주면 서버 기동 전에 화면을 빌드해 클래스패스의 `static/` 에 넣는다. Vite dev 서버는 꺼 두고 돌린다 (6절).

### C. 운영

```
브라우저 --> [도메인] --> 로드밸런서(:443) --> 서버 :19190 (Spring, prod 프로필)
                                               |- /, /assets/**  jar 안 static/
                                               +- /api/**        컨트롤러
```

jar 는 CI 에서 `bootJar` 로 만들고, 화면이 기본으로 들어간다. 인프라(AWS 구성, 배포 방식)는 `[ ]` 정하지 않았다.

---

## 2. 포트

| 용도 | 로컬 | 운영 |
|---|---|---|
| Spring (API + 화면) | 9190 | 19190 |
| Vite dev 서버 | 9191 | 없음 |
| MySQL (docker-compose) | 3306 | `[ ]` (RDS) |
| Redis (docker-compose) | 6379 | `[ ]` |

- 계약봄(9090/9091)과 포트를 겹치지 않게 91xx 를 쓴다. 두 프로젝트를 같은 PC 에서 동시에 띄울 수 있다.
- 운영에 화면 전용 포트는 없다. 화면은 Spring 과 같은 포트로 나간다.

---

## 3. Gradle 플래그

client/dist 를 담는 곳은 두 군데이고, 기본값이 서로 반대다.

| 담는 곳 | 기본 | 바꾸는 플래그 | 이유 |
|---|---|---|---|
| jar (`:server:bootJar`) | **담는다** | `-PskipClient` 로 뺀다 | CI·운영 jar 는 늘 화면을 가져야 한다. 빼먹는 일이 없게 기본을 "담는다"로 |
| 로컬 실행 (`bootRun`, IDE, `classes`) | **안 담는다** | `-PwithClient` 로 담는다 | 평소엔 Vite 가 화면을 맡는다. 서버를 띄울 때마다 npm 빌드가 돌지 않게 |

```bash
./gradlew build                          # 테스트 + 화면이 든 jar (CI 와 같음)
./gradlew build -PskipClient             # 테스트 + 화면 없는 jar (빠른 확인)
./gradlew :server:bootRun                # 로컬 개발 (A)
./gradlew :server:bootRun -PwithClient   # 로컬 통합 확인 (B)
```

설정 위치: server/build.gradle 맨 아래. Node 는 node-gradle 플러그인이 22.13.0 을 직접 받으므로 빌드 PC 에 Node 가 없어도 된다 (client/build.gradle).

---

## 4. 같은 오리진이라서 생기는 이점

### CORS 가 필요 없다

브라우저는 오리진이 다르면 CORS 검사를 한다. A·B·C 모두 브라우저에게는 오리진이 하나라 검사 자체가 없다. 그래서 Spring Security 에 CORS 설정을 두지 않는다.

다른 오리진에서 API 를 직접 불러야 하는 일이 생기면(예: 매장 POS 전용 앱, 본사 관리 도구를 다른 도메인에) 그때 그 오리진만 허용한다. 지금은 없다.

### 인증 쿠키가 운영과 똑같이 동작한다

JWT 는 HttpOnly 쿠키(`retailfnb_at`, `SameSite=Lax`, `path=/`)로 내려간다. 같은 오리진이면 브라우저가 쿠키를 알아서 붙이므로, 프론트는 토큰 저장 코드를 따로 두지 않는다.

- 로컬은 http 라 `Secure` 쿠키를 쓸 수 없어 local 프로필에서 `jwt.cookie.secure: false` 로 덮는다. 그 외 조건은 운영과 같다.
- 쿠키는 포트를 구분하지 않고 호스트(`localhost`) 기준으로 붙는다. 프록시 덕분에 브라우저는 9191 만 보므로 헷갈릴 일이 없다. 쿠키 이름을 프로젝트마다 다르게 둔 것도 이 때문이다.

### 화면과 API 버전이 늘 같다

운영에서는 화면과 API 가 같은 jar 에서 나오므로, 배포가 끝나면 둘이 함께 바뀐다. 단, 배포 직후 이미 열려 있던 화면은 예외다 (6절).

---

## 5. 프론트가 지킬 것

| 규칙 | 이유 |
|---|---|
| API 주소는 **상대 경로**(`/api/...`). `apiClient` 기본 주소는 빈 값 | 상대 경로여야 A 에서는 Vite 프록시를, B·C 에서는 Spring 을 그대로 탄다 |
| `.env.*` 에 의존하지 않는다 | `.env.*` 는 gitignore 라 CI 빌드에는 없다. 기본값이 운영에서 맞는 값이어야 한다 |
| 다른 서버를 직접 부를 때만 `VITE_API_BASE_URL` 에 절대 주소 | 이때는 다른 오리진이라 CORS 와 쿠키 조건이 달라진다 |
| 프록시 대상은 `/api` 만 | 화면 경로와 API 경로가 접두사로 나뉘어야 프록시와 SPA 포워딩이 단순하다 |

설정 위치: client/vite.config.ts 의 `server.proxy`, client/src/shared/core/apiClient.ts 의 `DEFAULT_API_BASE_URL`.

---

## 6. 주의할 점

| 상황 | 문제 | 대응 |
|---|---|---|
| Vite dev 서버를 띄운 채 `-PwithClient` 나 `bootJar` 실행 (Windows) | npmInstall 이 Vite 가 잡고 있는 네이티브 파일을 못 지워 EPERM | dev 서버를 끄고 돌린다. 기본 서버 실행은 npm 을 돌리지 않으므로 해당 없음 |
| 배포 직후 열려 있던 화면 | 새 jar 에는 이전 해시의 JS 파일이 없어 다음 화면 이동이 실패 | `main.tsx` 의 `vite:preloadError` 에서 새로고침. `index.html` no-cache `[ ]` |
| SPA 포워딩 컨트롤러를 만들 때 `[ ]` | 새로고침한 `/login` 같은 화면 경로가 404. 반대로 없는 JS 파일이 `index.html` 로 200 응답되면 MIME 오류 | `/api/**`, `/assets/**`, `/actuator/**`, 확장자 있는 경로는 포워딩하지 않는다 |
| A 방식에서 서버가 꺼져 있음 | `/api` 요청이 Vite 에서 502 | 서버를 띄운다 |
| `-PwithClient` 로 띄운 뒤 화면을 고침 | B 방식은 빌드된 화면이라 HMR 이 없다 | 화면을 고칠 때는 A 방식으로 돌아간다 |

## 체크리스트

- [ ] 새 API 가 `/api/v1/` 아래에 있는가?
- [ ] 프론트에서 API 주소를 절대 주소로 하드코딩하지 않았는가?
- [ ] CI 빌드(`./gradlew build`)에서 jar 에 화면이 들어가는가? (`BOOT-INF/classes/static/index.html`)
