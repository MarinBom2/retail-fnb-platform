# SOLID 원칙 — 매장 관리 플랫폼 적용 가이드

> 목적: 백엔드(Spring Boot 4.1 / Java 21 / JPA / MySQL)와 프론트엔드(React 19 / TypeScript)를 작성할 때
> 지켜야 할 객체지향 설계 원칙과, 이 프로젝트에서의 구체적인 적용 기준을 정리한다.
> 다섯 규칙(불변식은 한 클래스에, 확장점은 인터페이스 하나 + 빈 하나, 호출부 규약 고정, 실제로 호출되는 메서드만, 기술 타입은 경계를 넘지 않음)을 구체화한 문서다.
> 관련 문서: [domain-design.md](./domain-design.md), [develop-principal.md](./develop-principal.md)

## 한눈에 보기

| 원칙 | 이 프로젝트에서는 | 위반 신호 | 적용한 곳 |
|---|---|---|---|
| **S** 단일 책임 (SRP) | 함께 지켜야 하는 불변식(예: 재고 수량과 변동 기록)은 한 클래스에 둔다. 단일 책임을 이유로 정합성 규칙을 쪼개지 않는다 | 수량은 `Stock` 이 바꾸고 원장은 다른 서비스가 따로 쓴다 | `Stock`(수량 + `StockMovement`), `Order`(줄 + 합계), `Product`(SKU 옵션 규칙) |
| **O** 개방-폐쇄 (OCP) | 확장점은 인터페이스 하나 + 빈 하나 | 업종이 늘 때마다 서비스의 `if (businessType == ...)` 가 자란다 | `StockDeductionPolicy` + 업종별 빈 |
| **L** 리스코프 치환 (LSP) | 구현체를 바꿔도 호출부 규약이 같게 인터페이스에 못박는다 | 레시피가 없으면 한 구현체만 예외를 던진다 | "줄일 재고가 없으면 예외가 아니라 빈 목록" |
| **I** 인터페이스 분리 (ISP) | 실제로 호출되는 메서드만. 나중에 쓸 것 같은 메서드를 미리 넣지 않는다 | 리포지토리에 아무도 안 부르는 `findByXxx` | 리포지토리는 쓰는 쿼리만 (`RecipeRepository.findByMenuSkuId`) |
| **D** 의존 역전 (DIP) | 기술 타입이 경계를 넘지 않는다 | 도메인이 웹·SDK 타입을 import | 도메인은 JPA·Jackson 어노테이션만. 외부 SDK 는 기술 이름 하위 패키지에만 |

---

## S — 단일 책임 원칙 (Single Responsibility)

**정의**: 하나의 클래스는 하나의 액터(변경을 요구하는 주체)에 대해서만 책임진다. 이 프로젝트에서는 여기에 한 가지를 더한다. **함께 지켜야 하는 불변식은 한 클래스에 둔다.** 책임을 잘게 나누다가 정합성 규칙이 두 클래스로 쪼개지면, 한쪽만 고치는 실수가 생긴다.

**계층 책임**

| 계층 | 책임 | 바뀌는 이유 |
|---|---|---|
| `api` (Controller) | HTTP 요청/응답 변환, 인증 주체 추출, 입력 형식 검증 | API 스펙이 바뀔 때 |
| `application` (Service) | 유스케이스 흐름 조립, 트랜잭션 경계, 애그리거트 여러 개 조율 | 업무 흐름이 바뀔 때 |
| `domain` (Entity, VO, 포트) | 상태 전이, 불변식, 업종 규칙 | 업무 규칙이 바뀔 때 |
| 어댑터 (외부 SDK 하위 패키지) | 영속화, 외부 연동 기술 | DB·외부 API·SDK 가 바뀔 때 |

**나쁜 예**: 재고 수량과 원장 기록을 서로 다른 곳에서 한다. 누군가 `stock.setQuantity()` 만 부르면 원장이 어긋난다.

```java
@Service
public class InventoryService {
    @Transactional
    public void sell(Long storeId, Long skuId, long qty) {
        Stock stock = stockRepository.findByStoreIdAndSkuId(storeId, skuId).orElseThrow();
        stock.setQuantity(stock.getQuantity() - qty);           // 음수 검사도 여기, 다른 서비스에도 복사됨
        movementRepository.save(new StockMovement(stock.getId(), SALE, -qty)); // 빠뜨리기 쉽다
    }
}
```

**좋은 예**: 수량을 바꾸는 메서드가 원장 기록을 돌려준다. 기록 생성자는 패키지 전용이라 `Stock` 밖에서는 만들 수 없다.

```java
public StockMovement deductForSale(long amount, StockReference reference) {
    return decrease(MovementType.SALE, amount, reference);      // 음수 검사 + 수량 변경 + 기록 생성이 한 곳
}

// 서비스는 돌려받은 기록을 저장만 한다
stockMovementRepository.save(stock.deductForSale(quantity, StockReference.order(order.getId())));
```

같은 이유로 주문 합계는 `Order` 가 줄을 바꿀 때마다 다시 계산하고, SKU 옵션 규칙은 SKU 를 만드는 `Product.addSku` 한 곳에 있다.

**체크 질문**: "이 규칙을 깨려면 몇 개의 클래스를 고쳐야 하는가?" 하나가 아니면 불변식이 흩어진 것이다.

---

## O — 개방-폐쇄 원칙 (Open-Closed)

**정의**: 새 기능을 추가할 때 기존 코드를 수정하지 않고 새 코드를 추가하는 것으로 끝나야 한다.

**이 프로젝트의 확장 지점** ("인터페이스 하나 + 빈 하나")
- 판매가 줄이는 재고: `StockDeductionPolicy` (리테일 = 판 SKU, 외식 = 레시피 식자재)
- 그 밖의 업종 차이는 enum 규칙(데이터)으로 둔다: `ProductKind.isAllowedFor`, `OrderType.isAllowedFor`, `BusinessType.requiresApparelOption`. 행동이 아니라 "허용 여부"라서 인터페이스까지 갈 필요가 없다.

**나쁜 예**: 업종이 늘 때마다 결제 서비스가 자란다.

```java
if (order.getBusinessType() == BusinessType.FOOD_SERVICE) {
    recipeRepository.findByMenuSkuId(...)...      // 외식
} else {
    stockRepository...                             // 리테일
}
// 편의점(도시락은 레시피, 나머지는 SKU)을 더하면 이 메서드를 또 고친다
```

**좋은 예**: 정책을 인터페이스로 뽑고, 업종마다 빈 하나를 둔다. 결제 서비스는 정책을 고르기만 한다.

```java
public interface StockDeductionPolicy {
    BusinessType businessType();
    List<StockRequirement> requirementsFor(Long soldSkuId, long soldQuantity);
}

@Configuration
public class InventoryPolicyConfig {
    @Bean StockDeductionPolicy generalMerchandiseDeduction() { return new DirectSkuDeduction(GENERAL_MERCHANDISE); }
    @Bean StockDeductionPolicy apparelDeduction()            { return new DirectSkuDeduction(APPAREL); }
    @Bean StockDeductionPolicy foodServiceDeduction(RecipeRepository r) { return new RecipeStockDeduction(r); }
}

// OrderPaymentService
StockDeductionPolicy policy = stockDeductionPolicies.of(order.getBusinessType());
```

`StockDeductionPolicies` 는 기동할 때 업종마다 정책이 정확히 하나인지 확인한다. 업종을 더하고 빈을 빠뜨리면 첫 결제가 아니라 기동에서 실패한다.

**주의**: 변경 지점이 실제로 두 개 이상일 때 추상화한다. 위 확장점 밖의 "미래를 위한" 인터페이스는 만들지 않는다 (YAGNI).

---

## L — 리스코프 치환 원칙 (Liskov Substitution)

**정의**: `S` 가 `T` 의 하위 타입이면, `T` 를 쓰는 코드는 `S` 로 바꿔도 올바르게 동작해야 한다. 구현체는 사전 조건을 강화하지 않고, 사후 조건을 약화하지 않는다. 그래서 **규약을 인터페이스 Javadoc 에 못박는다.**

**위반 신호**
- 구현체 메서드가 `UnsupportedOperationException` 을 던진다.
- 호출하는 쪽이 `instanceof` 로 구현체를 구분한다.
- 같은 상황(줄일 재고 없음)을 구현체마다 다르게(예외 / null / 빈 목록) 알린다.

**나쁜 예**: 레시피가 없을 때 외식 정책만 예외를 던져, 결제 서비스가 업종을 보고 try-catch 를 다르게 한다.

```java
class RecipeStockDeduction implements StockDeductionPolicy {
    public List<StockRequirement> requirementsFor(Long skuId, long qty) {
        Recipe recipe = recipeRepository.findByMenuSkuId(skuId)
            .orElseThrow(() -> new IllegalStateException("레시피 없음"));   // DirectSkuDeduction 은 예외가 없다
        ...
    }
}
```

**좋은 예**: 규약을 인터페이스에 적고 모든 구현체가 지킨다.

```java
/**
 * 규약 (구현체를 바꿔도 같다)
 *   - 돌려준 목록에 같은 SKU 가 두 번 나오지 않는다
 *   - 줄일 재고가 없으면 예외가 아니라 빈 목록이다 (예: 레시피를 아직 등록하지 않은 메뉴)
 */
public interface StockDeductionPolicy { ... }
```

**JPA 관련 주의**: 업종별로 엔티티를 상속(`@Inheritance`, `ApparelProduct extends Product`)하지 않는다. **`businessType`·`kind` 필드 + enum 규칙 + 전략 객체** 조합을 쓴다. 상속하면 `Order` 가 `Product` 하위 타입마다 다르게 동작해야 하는 순간 LSP 가 깨진다.

---

## I — 인터페이스 분리 원칙 (Interface Segregation)

**정의**: 클라이언트는 자신이 사용하지 않는 메서드에 의존하도록 강요받아서는 안 된다. 이 프로젝트에서는 "실제로 호출되는 메서드만 넣는다".

**나쁜 예**: 언젠가 쓸 것 같은 조회를 리포지토리에 미리 넣는다.

```java
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByStoreIdAndSkuId(Long storeId, Long skuId);
    List<Stock> findByStoreId(Long storeId);                         // 아무도 안 부른다
    List<Stock> findByQuantityLessThan(long threshold);              // "재고 알림 만들 때 쓰려고"
    Page<Stock> findBySkuIdIn(Collection<Long> ids, Pageable p);    // 아무도 안 부른다
}
```

**좋은 예**: 지금 호출하는 메서드만 둔다. 재고 화면이 생기면 그때 그 화면이 쓰는 조회를 더한다.

```java
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByStoreIdAndSkuId(Long storeId, Long skuId);   // OrderPaymentService
}
```

- 포트(`StockDeductionPolicy`)도 같다. 차감량을 묻는 메서드 하나뿐이고, 업종 이름·설명 같은 표시용 메서드는 넣지 않는다.
- 매출 집계처럼 읽기 모양이 다른 조회는 별도 조회 리포지토리로 나눈다 `[ ]`.

---

## D — 의존 역전 원칙 (Dependency Inversion)

**정의**: 고수준 모듈(업무 규칙)은 저수준 구현(기술)이 아니라 추상에 의존한다. 이 프로젝트에서는 "**기술 타입이 경계를 넘지 않는다**".

**경계 규칙**

| 위치 | import 해도 되는 것 | 하면 안 되는 것 |
|---|---|---|
| `*.domain` (common:core) | JPA·Jackson 어노테이션, `java.*`, 같은 모듈의 다른 도메인 | `jakarta.servlet`, Spring Web, `HttpStatus`, 외부 SDK |
| `*.application` (server) | 도메인, 포트, 리포지토리 | 외부 SDK 타입 |
| 외부 SDK 어댑터 | SDK 타입 | (SDK 타입을 반환·인자로 바깥에 내보내기) |

- 예외: `Member` 는 인증 주체라 `spring-security-core` 의 `UserDetails` 를 구현한다. 웹 시큐리티(필터 체인)는 server 에만 있다.
- 외부 SDK(결제 PG, 메시지, 파일 저장소 등)를 붙일 때는 `common:core` 에 `implementation`(모듈 밖으로 안 보임)으로 넣고, 타입은 그 기술 이름의 하위 패키지(예: `payment/toss`, `storage/s3`) 아래에만 둔다. 도메인에는 포트 인터페이스만 둔다.
- `ErrorCode` 의 `status` 가 `int` 인 이유도 이것이다. `common:core` 에는 `spring-web` 이 없어서 `HttpStatus` 를 쓸 수 없다. 변환은 server 의 `ProblemDetails` 가 한다.

**주입 규칙**
- 생성자 주입만 사용한다. 필드 주입(`@Autowired` 필드)과 세터 주입은 금지한다.
- Lombok `@RequiredArgsConstructor` + `private final` 필드 조합을 표준으로 한다.
- 순환 의존이 생기면 설계를 다시 본다. `@Lazy` 로 덮지 않는다.
- 현재 시각은 `Clock` 빈으로 얻는다 (`Instant.now()` 를 직접 부르지 않는다). 테스트가 시각을 고정할 수 있다.

---

## 프론트엔드(React)에서의 SOLID

| 원칙 | React 적용 |
|---|---|
| SRP | 컴포넌트는 "그리기"만, 데이터 로딩 로직은 커스텀 훅으로, HTTP 는 `shared/api/` 모듈로 분리 |
| OCP | 업종별로 다른 화면 블록(예: 의류 사이즈 선택, 외식 테이블 선택)은 `businessType` 별 컴포넌트를 매핑 테이블로 추가, 분기문 추가 지양 |
| LSP | 같은 props 타입을 받는 컴포넌트끼리는 교체 가능해야 함 |
| ISP | 컴포넌트가 필요한 필드만 props 로 받는다. 거대한 `order` 객체 통째로 넘기기 지양 |
| DIP | 컴포넌트가 `fetch` 를 직접 호출하지 않고 `shared/api/*` 를 통해 호출 (테스트 시 대역 지점) |

---

## 코드 리뷰 체크리스트

- [ ] 함께 지켜야 하는 불변식(수량·기록, 줄·합계, 옵션 규칙)이 한 클래스에 있는가? (SRP)
- [ ] 업종을 추가할 때 서비스의 분기문을 고치지 않아도 되는가? 업종 차이가 §3.2 의 세 군데 밖으로 새지 않았는가? (OCP)
- [ ] 구현체 중 "지원 안 함" 예외를 던지거나, 같은 상황을 다르게 알리는 것이 있는가? (LSP)
- [ ] 리포지토리·포트에 실제로 호출되지 않는 메서드가 있는가? (ISP)
- [ ] `*.domain` 이 웹·외부 SDK 타입을 import 하는가? (DIP, 경계)
- [ ] 생성자 주입을 쓰는가? 필드 주입이 남아 있는가?
- [ ] 추상화가 실제 변경 지점 없이 "미래를 위해" 만들어진 것은 아닌가? (과적용 경계)
