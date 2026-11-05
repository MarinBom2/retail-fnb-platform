# 개발 규약 — 매장 관리 플랫폼 코드 작성 규칙

> 목적: SOLID 같은 일반 원칙이 아니라, 이 프로젝트에서 코드를 쓸 때 **매번 같은 방식으로** 해야 하는 구체적인 규칙을 모은다.
> 새 규칙이 정해지면 이 문서에 먼저 추가하고, 기존 코드는 해당 영역을 건드릴 때 맞춘다.
> 관련 문서: [solid.md](./solid.md), [domain-design.md](./domain-design.md), [authentication-security.md](./authentication-security.md), [runtime-structure.md](./runtime-structure.md)

## 한눈에 보기

| 영역 | 규칙 | 근거 |
|---|---|---|
| enum | 모든 enum 은 `DescriptionCode` 를 구현하고 한글 `description` 을 가진다. 프론트에는 `{code, description}` 으로 내려간다 | 표시 문구를 서버 한 곳에서 관리 |
| 예외 | 도메인 규칙 위반 예외는 해당 도메인 패키지(`order.domain.exception`)에 두고 `DomainException` 을 상속한다. 모두 unchecked | 의존 방향(domain 이 바깥을 모름), 자동 롤백 |
| 요구사항 추적 | 코드에 `FR-…`, `SCR-…` ID 를 주석·라우트 이름에 남긴다 | 요구사항 정의서·화면설계서를 개발 후 산출물로 뽑음 |
| 주석 | 필요한 곳에만 한두 줄. 날짜를 넣지 않는다. HTML 태그를 쓰지 않는다 | 날짜·경위는 git 기록과 md 문서가 맡는다 |
| 금액 | `Money`(원 단위 `long`). `double`·`BigDecimal` 을 섞지 않는다 | 원화는 소수가 없다. 부동소수점 오차 없음 |
| JWT | jjwt 0.13 + `jjwt-gson`. `jjwt-jackson` 은 쓰지 않는다 | Boot 4 의 Jackson 3(`tools.jackson`)와 Jackson 2 가 섞임 |
| 모듈 | 도메인·공통 yml 은 `common:core`, API 는 `server`, 배치는 `batch`. 앱 모듈끼리 의존하지 않는다 | server·batch 가 같은 도메인을 씀 |
| 비밀 값 | `src/main/resources` 에 두지 않는다. 로컬은 루트 `secrets/`, 운영은 환경변수 | resources 에 두면 jar·Docker 이미지에 들어감 |
| API 응답·오류 | 성공은 DTO 그대로, 오류는 ProblemDetail(RFC 9457) + `code`(`ErrorCode`). 시큐리티 401/403 도 같은 형식 | 표준 형식, Spring 기본 오류와 통일 |
| API 경로·문서 | 모든 API 는 `/api/v1/...`, 관리자는 `/api/v1/admin/...`. 컨트롤러에 `@Tag`, 메서드에 FR ID 로 시작하는 `@Operation` | 프록시·포워딩·시큐리티가 `/api` 접두사 기준 |
| 작업 순서 | 새 영역은 뼈대(패키지·시그니처·불변식 테스트)를 먼저 잡고 구현을 채운다 | 구조 결정을 먼저 리뷰할 수 있음 |

---

## 1. enum: code + description

### 규칙

- 서버의 **모든 enum** 은 `com.marinbom2.retailfnbplatform.type.common.DescriptionCode` 를 구현한다.
- 한글 `description` 필드를 가진다. 화면에 그대로 나가는 문구다.
- `@JsonFormat(shape = Shape.OBJECT)` 를 붙여 JSON 으로 `{"code": "APPAREL", "description": "의류 리테일"}` 형태로 직렬화한다.
- 클라이언트(React)는 `code` 로 분기·비교하고 `description` 으로 표시한다. 프론트에 한글 문구를 중복 정의하지 않는다.
- 업종에 따라 허용 여부가 갈리는 규칙은 enum 메서드로 둔다 (`ProductKind.isAllowedFor`, `OrderType.isAllowedFor`). `switch` 식으로 써서 새 값을 빠뜨리면 컴파일 에러가 나게 한다 ([domain-design.md](./domain-design.md) §3.2).

### 서버 작성 형태

```java
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum OrderType implements DescriptionCode {

    COUNTER("계산대"),
    DINE_IN("매장 식사"),
    TAKEOUT("포장");

    private final String description;

    public boolean isAllowedFor(BusinessType businessType) {
        return switch (businessType) {
            case GENERAL_MERCHANDISE, APPAREL -> this == COUNTER;
            case FOOD_SERVICE -> this == DINE_IN || this == TAKEOUT;
        };
    }

    @Override
    public String getDescription() {
        return description;
    }
}
```

`DescriptionCode` 의 `getCode()` 는 기본 구현이 `Enum.name()` 을 돌려주므로 enum 쪽에서 따로 구현하지 않는다.

### 클라이언트 타입 형태

```ts
/** 서버 enum 은 {code, description} 객체로 온다. 비교는 code, 표시는 description */
export interface CodeLabel {
  code: string
  description: string
}
```

---

## 2. 예외

### 위치

| 종류 | 패키지 | 예 |
|---|---|---|
| 도메인 규칙 위반 | 각 도메인의 `domain.exception`, `domain.base.DomainException` 상속 | `InsufficientStockException`, `IllegalOrderStateException`, `InvalidCatalogException` |
| 계층 공통 (조회 실패, 권한, 입력 형식) | core `exception.BusinessException` + `type.common.ErrorCode` | `throw new BusinessException(ErrorCode.NOT_FOUND_DATA)`. 예외 클래스를 늘리지 않고 ErrorCode 항목을 늘린다 |
| 외부 연동 실패 | 어댑터 패키지 안에서 잡아 포트 규약의 예외로 바꿔 던짐 | `[ ]` (결제 PG 를 붙일 때) |

### 형태

- 모두 `RuntimeException` 계열(unchecked). `@Transactional` 안에서 던지면 자동 롤백된다. 결제 중 재고가 모자라면 주문 상태까지 함께 롤백되는 것이 이 규칙 덕분이다.
- 호출부가 **타입으로 분기**한다. 메시지 문자열을 파싱하지 않는다. 분기에 필요한 값(`InsufficientStockException.skuId`)은 필드로 노출한다.
- 도메인 예외 메시지는 **로그용**이다. 화면 문구는 `GlobalExceptionHandler` 가 `ErrorCode` 로 바꾼다 (7절).

---

## 3. 요구사항·화면 ID와 주석

- 요구사항 ID `FR-XX-NNN`, 화면 ID `SCR-NN` 은 [domain-design.md](./domain-design.md) §9 의 표를 기준으로 한다.
  - `AU` 인증, `ST` 브랜드·매장, `CA` 상품(카탈로그), `IN` 재고, `OR` 주문·결제
- 클래스·메서드 Javadoc, 컨트롤러 라우트 이름, 클라이언트 `ROUTES` 키(`SCR01_HOME`)에 ID 를 남긴다.
- ID 가 없는 기능을 구현하게 되면 domain-design.md 표에 먼저 행을 추가한다.

### 주석

- 코드만 봐서는 알 수 없는 이유·규약·주의점이 있는 곳에만 단다. 코드를 그대로 풀어 쓰는 주석은 달지 않는다.
- 한두 줄로 짧게. 긴 설명은 md 문서에 두고 주석에는 문서 위치(`md/domain-design.md §5.4`)만 적는다.
- 날짜를 넣지 않는다. 언제·누가 바꿨는지는 git 이 기록한다.
- Javadoc·yml 주석에 HTML 태그(`<p>`, `<ul>`, `<li>`, `<pre>`)를 쓰지 않는다.

---

## 4. 의존성 결정 기록

| 라이브러리 | 결정 | 이유 |
|---|---|---|
| Spring Boot 4.1.2, Java 21 | 루트 `build.gradle` 에서 플러그인 버전만 정하고 `apply false` | 모듈마다 같은 버전 |
| jjwt 0.13.0 | `jjwt-api` + `jjwt-impl` + `jjwt-gson` | `jjwt-jackson` 0.13.0 이 Jackson 2 에 의존. Boot 4.1 은 Jackson 3 라 두 Jackson 이 classpath 에 공존하게 됨 |
| springdoc-openapi 3.2.0 | `springdoc-openapi-starter-webmvc-ui`, server 에서 `implementation` | Boot 4 라인. 내부 swagger-core 가 Jackson 2 를 가져오지만 패키지가 달라 충돌하지 않는다 |
| Testcontainers 2.x | `testcontainers-mysql`, `testcontainers-junit-jupiter` | Boot 4.1.2 가 관리. 클래스가 1.x 와 다름 (`org.testcontainers.mysql.MySQLContainer`) |
| spring-boot-starter-data-redis | server 에서 `implementation` | 캐시·세션 외 용도 `[ ]`. 헬스체크에서는 뺀다 (`management.health.redis.enabled=false`) |
| `java-test-fixtures` | common:core 에 적용, server 가 `testFixtures(project(':common:core'))` | 도메인 픽스처(`DomainFixtures`)를 모듈끼리 복사하지 않는다 |
| node-gradle 7.1.0 | `client/build.gradle`, Node 22.13.0 을 플러그인이 받는다 | 빌드 PC 에 Node 가 없어도 jar 에 화면을 담을 수 있다 |

---

## 5. 모듈 구조

```
retail-fnb-platform (루트 build.gradle: 플러그인 버전 + Java 모듈 공통 설정만)
 +- common/core   java-library. 도메인(member, store, catalog, inventory, order), 공통 yml. bootJar 끔
 +- server        Boot 실행 jar. API·보안·화면(client dist 포함). core 의존
 +- batch         Boot 실행 jar. 배치 작업. core 의존. server 는 의존하지 않는다
 +- client        Vite + React (node-gradle). server 의 bootJar 가 dist 를 static/ 으로 담는다
```

- 패키지 이름은 모듈과 무관하게 [domain-design.md](./domain-design.md) §11 을 따른다. 앱 클래스(`ServerApplication`, `BatchApplication`)는 `com.marinbom2.retailfnbplatform` 루트에 둬서 core 의 엔티티·설정까지 스캔한다.
- 설정 파일 `application*.yml` 은 core 에만 둔다. server·batch 에 같은 이름의 파일을 두면 클래스패스에서 하나만 읽혀 core 설정(DB·JWT)이 사라진다. 앱 이름처럼 모듈마다 다른 값은 main 클래스의 `setDefaultProperties` 로 넣는다.
- core 는 도메인이 드러내는 JPA·Jackson 어노테이션만 `api` 로 내보낸다. 외부 SDK 를 붙이면 `implementation` 으로 넣어 server·batch 에서 import 할 수 없게 한다.
- 빌드는 루트에서 한다. 화면을 어디에 담는지와 `-PskipClient`·`-PwithClient` 플래그는 [runtime-structure.md](./runtime-structure.md).

```bash
./gradlew build -PskipClient
```

Windows PowerShell 은 `.\gradlew.bat build -PskipClient`.

### 애그리거트 규칙

- 애그리거트끼리는 ID 로만 참조한다. `@ManyToOne` 은 같은 애그리거트 안(`Sku`→`Product`, `OrderLine`→`Order`)에서만 쓴다.
- 자식 엔티티(`Sku`, `OrderLine`, `StockMovement`)의 생성자는 패키지 전용이고 루트만 만든다.
- 애그리거트 여러 개를 함께 바꾸는 일(결제 + 재고)은 server 의 `*.application` 서비스 한 곳에서, 한 트랜잭션으로 한다.
- 업종이 바뀌지 않는 값(`businessType`)은 필요한 엔티티에 복사해 두고 `updatable = false` 로 막는다.

### enum 칼럼 스키마

- `NoColumnCheckMySQLDialect` 가 `@Enumerated(STRING)` 칼럼을 varchar 로 만들고 값 목록 CHECK 제약을 만들지 않는다. Hibernate 7 기본은 MySQL 네이티브 `enum(...)` 이라, ddl-auto=update 에서 enum 값을 추가하면 새 값 저장이 막힌다.

---

## 6. 비밀 값

| 값 | 로컬 | 운영 |
|---|---|---|
| JWT 서명 키 | 루트 `secrets/application-secret.yml` (비우면 기동마다 임시 키) | `JWT_SECRET` (없으면 prod 기동 실패) |
| DB 접속 | `application-local.yml` (docker-compose 의 root/root, 비밀 아님) | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| Redis | `localhost:6379` | `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` |

- `secrets/` 는 gitignore 이고, server·batch 의 `bootRun` 클래스패스에만 붙는다. jar 에는 들어가지 않는다.
- `application-local.yml` 은 커밋한다. 비밀이 아닌 로컬 값(포트, docker-compose DB, Secure 쿠키 끄기)만 둔다.
- 새 비밀 값이 생기면 이 표에 행을 추가하고 `application-prod.yml` 머리 주석의 환경변수 목록도 고친다.

---

## 7. API 응답과 오류 (ProblemDetail)

공통 응답 봉투(`ApiResponse`)를 쓰지 않는다. **성공은 DTO 그대로, 오류는 RFC 9457 Problem Details** 로 응답한다.

### 형식

| 경우 | HTTP 상태 | 본문 |
|---|---|---|
| 성공 | 2xx | DTO 그대로. 본문이 없으면 204 |
| 오류 | 4xx·5xx | `application/problem+json`. `ProblemDetail` + 확장 필드 `code` |

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "재고가 부족합니다.",
  "instance": "/api/v1/orders/12/payment",
  "code": "OUT_OF_STOCK",
  "skuId": 31
}
```

- `detail` 은 화면에 그대로 나가는 문장이다. 내부 정보(남은 수량, 예외 원문, 쿼리, 스택)를 넣지 않는다.
- `code` 는 서버 `ErrorCode` 이름이다. 클라이언트는 문구가 아니라 이 값으로 분기한다.
- 입력값 검증 실패는 `errors: [{field, message}]`, 재고 부족은 `skuId` 를 함께 보낸다.

### 서버 구성

| 클래스 | 모듈·패키지 | 역할 |
|---|---|---|
| `ErrorCode` | core `type.common` | 오류 목록. HTTP 상태(int), `code`(이름), 기본 문구 |
| `BusinessException` | core `exception` | 애플리케이션 계층 공통 예외 |
| `DomainException` | core `domain.base` | 도메인 규칙 위반의 공통 부모 |
| `ProblemDetails` | server `exception` | `ErrorCode` 로 `ProblemDetail` 을 만드는 **유일한 곳** |
| `GlobalExceptionHandler` | server `exception` | 컨트롤러 단계 오류 전부. Spring MVC 예외에도 `code` 와 한글 문구를 입힌다 |
| `SecurityErrorResponder` | server `security` | 시큐리티 필터 단계의 401/403 ([authentication-security.md](./authentication-security.md) 7절) |

예외를 응답으로 바꾸는 규칙:

| 예외 | ErrorCode | 비고 |
|---|---|---|
| `BusinessException` | 담긴 값 | 메시지가 `detail` 이 된다 |
| `InsufficientStockException` | `OUT_OF_STOCK` (409) | `skuId` 추가 |
| `IllegalOrderStateException` | `ORDER_STATE_CONFLICT` (409) | |
| `IllegalStoreStateException` | `STORE_STATE_CONFLICT` (409) | |
| `OptimisticLockingFailureException` | `CONCURRENT_UPDATE` (409) | 재고·주문 동시 수정. 다시 시도 |
| 그 밖의 `DomainException` | `DOMAIN_RULE_VIOLATION` (400) | 예외 메시지는 로그로만 |
| `@Valid` 실패 | `INVALID_REQUEST_DATA` (400) | `errors` 추가 |
| Spring MVC 예외 | 상태별 기본값 (`ErrorCode.defaultFor`) | Spring 의 영문 `detail` 은 로그로만 |
| 그 밖의 예외 | `INTERNAL_ERROR` (500) | 스택은 로그로만 |

### 클라이언트

| 파일 | 역할 |
|---|---|
| `shared/types/common.ts` | `ProblemDetail`, `FieldError` 타입 (서버와 1:1) |
| `shared/core/apiClient.ts` 의 `apiRequest` | 모든 호출의 공통 수신부. 2xx 면 DTO 그대로, 아니면 `ApiError`. 401 이면 세션 만료를 알린다 |
| `shared/utils/errorMessage.ts` | 화면에 보여 줄 문장 하나를 고른다 |

```ts
try {
  await orderApi.pay(orderId, request)
} catch (e) {
  if (e instanceof ApiError && e.code === 'OUT_OF_STOCK') {
    // e.problem?.skuId 로 모자란 상품 표시
  }
  setError(errorMessage(e, '결제하지 못했습니다.'))
}
```

### 테스트

- `GlobalExceptionHandlerTest`: 상태, `code`, `detail`, 확장 필드가 최상위인지, 내부 메시지가 새지 않는지.
- `SecurityErrorResponderTest`: Boot 의 `JsonMapper` 로 401·403 을 썼을 때 같은 형식인지.
- `AuthFlowTest`: 실제 필터 체인으로 401·403·로그인 실패·로그아웃 뒤 토큰 거절·필터 순서 (Docker 가 있을 때).

---

## 8. 컨트롤러와 API 경로 (Swagger)

### 경로

| 대상 | 경로 | 예 |
|---|---|---|
| 사용자 API | `/api/v1/{리소스}` | `/api/v1/stores/{storeId}/orders`, `/api/v1/orders/{orderId}/payment` |
| 인증 | `/api/v1/auth/...` | `/api/v1/auth/login`, `/api/v1/auth/logout` |
| 내 정보 | `/api/v1/me` | |
| 관리자 API | `/api/v1/admin/...` | `/api/v1/admin/brands` |

- 모든 API 는 `/api/` 로 시작한다. Vite 프록시, 시큐리티 체인이 이 접두사를 기준으로 한다 ([runtime-structure.md](./runtime-structure.md)).
- 리소스는 복수 명사, 여러 단어면 kebab-case. 하위 리소스는 `/{부모}/{id}/{자식}`. 상태 전이처럼 리소스로 표현하기 어려운 동작만 `/open`, `/cancel`, `/refund` 같은 하위 경로를 쓴다.
- 응답은 7절을 따른다. 성공은 DTO 그대로 (`201`·`204` 는 `@ResponseStatus` 로).

### Swagger 규칙

| 붙이는 곳 | 어노테이션 | 쓰는 법 |
|---|---|---|
| 컨트롤러 클래스 | `@Tag(name, description)` | `name` 은 화면 기준 한글 이름(예: "주문"), `description` 에 SCR ID |
| 컨트롤러 메서드 | `@Operation(summary, description)` | `summary` 는 한 줄, `description` 은 **FR ID 로 시작** |
| 엔드포인트 고유 오류 | `@ApiResponse(responseCode, description, content)` | `description` 에 `ErrorCode` 이름. 공통 오류(401·403·500)는 적지 않는다 |
| 요청·응답 DTO 필드 | `@Schema(description, example)` | `example` 에 실제 개인정보를 쓰지 않는다 |

- 문서에는 `/api/**` 만 나온다 (`springdoc.paths-to-match`). 로컬은 `http://localhost:9190/swagger-ui.html`, 운영은 끈다.
- 로그인은 컨트롤러가 아니라 시큐리티 필터라 springdoc 이 찾지 못한다. `OpenApiConfig` 가 직접 적어 넣는다.

---

## 9. 작업 순서

1. 새 영역은 **뼈대 먼저**: 패키지, 클래스 이름, 포트 시그니처, 상태 전이, 불변식 테스트.
2. 뼈대를 리뷰한 뒤 구현(서비스·API·화면)을 채운다.
3. 단위 테스트는 인프라 없이 돌아가는 도메인 테스트를 먼저(`DomainFixtures`), 통합 테스트(Testcontainers)는 API 가 생긴 뒤.
4. 규칙 클래스마다 경계값 테스트를 둔다 (재고: 남은 수량 == 뺄 수량 / +1, 결제: 합계 / 합계 −1원).

---

## 코드 리뷰 체크리스트

- [ ] 새 enum 이 `DescriptionCode` 를 구현하고 `@JsonFormat(shape = OBJECT)` 가 붙어 있는가?
- [ ] 업종 분기가 enum 규칙·`StockDeductionPolicy` 밖에서 새로 생기지 않았는가? (1절, domain-design.md §3.2)
- [ ] 프론트에서 enum 문구를 하드코딩하지 않고 `description` 을 쓰는가? 비교는 `code` 로 하는가?
- [ ] 도메인 예외가 도메인 패키지에 있고 `DomainException` 을 상속하는가? (2절)
- [ ] 예외 분기가 타입 기반인가? (메시지 문자열 비교 없음)
- [ ] 금액이 `Money` 인가? `double` 이 섞이지 않았는가?
- [ ] 애그리거트끼리 ID 로만 참조하는가? 자식 엔티티를 루트 밖에서 만들지 않는가? (5절)
- [ ] 새 클래스·라우트에 `FR-`/`SCR-` ID 가 있는가? (3절)
- [ ] 주석이 필요한 곳에만 짧게 있고, 날짜·HTML 태그가 없는가? (3절)
- [ ] 새 API 경로가 `/api/v1/` 아래인가? (8절)
- [ ] 새 의존성을 추가했다면 4절 표에 이유를 적었는가?
- [ ] 비밀 값이 `src/main/resources` 나 yml 리터럴에 들어가지 않았는가? (6절)
- [ ] server·batch 가 서로를 의존하지 않는가? (5절)
- [ ] 오류 응답이 ProblemDetail + `code` 인가? `detail` 에 내부 정보가 없는가? (7절)
- [ ] 권한 부족을 401 이 아니라 403 으로 보내는가? (7절)
