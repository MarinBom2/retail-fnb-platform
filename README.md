# retail-fnb-platform

생활용품(다이소)·의류(지오다노)·외식(음식점) 매장을 한 모델로 관리하는 **매장 관리 플랫폼**. 브랜드와 매장, 상품(SKU), 매장 재고, 판매(주문·결제)를 다룬다.

- 제품 전략 (B2B): [`md/product-strategy.md`](./md/product-strategy.md) — 고객, 약속, 데모 시나리오, 가격 가설
- API 계약: [`md/api.md`](./md/api.md)
- 도메인 설계: [`md/domain-design.md`](./md/domain-design.md) — 세 업종의 차이를 어디에 담는지, 애그리거트와 불변식, 결제와 재고의 트랜잭션
- 개발 규약: [`md/develop-principal.md`](./md/develop-principal.md), [`md/solid.md`](./md/solid.md)
- 인증: [`md/authentication-security.md`](./md/authentication-security.md) — JWT HttpOnly 쿠키, 시큐리티 필터 구조
- 실행·빌드: [`md/runtime-structure.md`](./md/runtime-structure.md)

## 구조

```
retail-fnb-platform/
├── common/core/  도메인(member, store, catalog, inventory, order), 공통 예외·설정·yml  (라이브러리 jar)
├── server/       Spring Boot 4.1.2 API + 시큐리티 + React 빌드 결과 서빙            (server.jar)
├── batch/        Spring Batch                                                     (batch.jar)
├── client/       Vite + React 19 + TypeScript 5.9 SPA
├── md/           설계·규약 문서
└── docker-compose.yml  로컬 MySQL 8.4, Redis 7
```

Java 21, Gradle 멀티모듈. 앱 모듈(server, batch)끼리는 의존하지 않고 둘 다 `common:core` 를 쓴다.

## 데모 실행 (DB 없이)

```bash
SPRING_PROFILES_ACTIVE=demo ./gradlew :server:bootRun -PwithClient
```

http://localhost:9190 → "데모로 체험하기". 계정 `demo@retailfnb.dev` / `Demo1234!` (가상 브랜드 3개, 매장 8곳의 점주), 직원 권한은 `staff@retailfnb.dev`. 메모리 DB(H2)라 재기동하면 데이터가 처음으로 돌아간다. Windows PowerShell 은 `$env:SPRING_PROFILES_ACTIVE="demo"; .\gradlew.bat :server:bootRun -PwithClient`.

## 로컬 실행 (MySQL)

```bash
docker compose up -d
```

```bash
./gradlew :server:bootRun          # http://localhost:9190 (local 프로필, Swagger: /swagger-ui.html)
```

```bash
cd client && npm install && npm run dev   # http://localhost:9191 (/api 는 9190 으로 프록시)
```

Windows PowerShell 은 `.\gradlew.bat :server:bootRun`.

비밀 값은 저장소에 넣지 않는다. 로컬은 루트 `secrets/application-secret.yml`(gitignore, bootRun 클래스패스에만 붙음), 운영은 환경변수(`JWT_SECRET`, `DB_URL` …). 목록은 `md/develop-principal.md` §6.

## 빌드·테스트

```bash
./gradlew build -PskipClient   # 화면 없이 테스트 + jar
```

```bash
./gradlew build                # 화면이 든 server.jar (CI 와 같음)
```

통합 테스트 중 `DemoScenarioTest` 는 demo 프로필(H2)로 실제 API 흐름을 돌려 Docker 없이 돈다. `AuthFlowTest`, `*ApplicationTests` 는 Testcontainers MySQL 을 쓰고 Docker 가 없으면 건너뛴다.
