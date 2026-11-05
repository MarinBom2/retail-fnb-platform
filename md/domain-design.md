# 매장 관리 도메인 설계 — 생활용품·의류·외식 (다이소 · 지오다노 · 음식점)

> 상태: **초안**. 결정 반영: 브랜드 단일가, 식자재 음수 재고 불가, 매장 간 이동은 요청 → 승인 → 출고 → 입고. 세 업종을 하나의 모델로 담고, 업종마다 다른 부분만 데이터(enum 규칙)와 확장점(인터페이스 하나 + 빈 하나)으로 나눈다.
> 관련 문서: [develop-principal.md](./develop-principal.md), [solid.md](./solid.md), [authentication-security.md](./authentication-security.md), [runtime-structure.md](./runtime-structure.md)
> 모르는 값과 아직 정하지 않은 것은 `[ ]`로 비워 둔다. 구현된 것은 **(구현)**, 설계만 한 것은 **(설계)**로 표시한다.

## 1. 한 줄 정의와 범위

브랜드 본사와 매장이 **상품(SKU)·매장 재고·판매(주문·결제)**를 한곳에서 관리하는 플랫폼. 생활용품(다이소), 의류(지오다노), 외식(음식점) 세 업종을 같은 모델로 다루고, 업종 차이는 "어떤 상품을 만들 수 있는지", "어떤 주문을 받는지", "판매가 어떤 재고를 줄이는지" 세 군데에서만 갈린다.

| 구분 | 지금 (이번 뼈대) | 다음 단계 |
|---|---|---|
| 브랜드·매장 | 브랜드 등록, 매장 생성·영업 시작·종료, 매장 직원 역할 **(구현)** | 매장별 영업시간, 휴무일 |
| 상품 | 상품 마스터 + SKU, 의류 색상×사이즈, 외식 메뉴·식자재·레시피 **(구현)** | 매장별 판매가, 할인·행사, 분류 체계 |
| 재고 | 매장×SKU 수량, 변동 원장, 입고·판매·환불·실사, 안전재고, 다른 매장 재고 찾기, **매장 간 이동(요청→승인→출고→입고)** **(구현)** | 본사 발주, 유통기한·로트 |
| 판매 | 계산대·매장 식사·포장 주문, 결제(전액)·취소·환불, 결제 시 재고 차감 **(구현)** | 부분 결제·더치페이, 주방 진행 상태, 영수증 번호 |
| 대시보드 | 오늘 매출·시간대별 매출·인기 상품·안전재고 부족(+재고 많은 매장 추천)·처리할 이동 **(구현)** | 기간 비교, 브랜드 전체(본사) 대시보드 |
| API·화면 | 인증, 매장·대시보드·카탈로그·재고·이동·주문 API ([api.md](./api.md)), 화면 SCR-01~09 **(구현)** | 상품 등록·브랜드 관리(본사) 화면 |

## 2. 용어 사전

| 용어 | 영문 | 뜻 |
|---|---|---|
| 업종 | BusinessType | `GENERAL_MERCHANDISE`(생활용품), `APPAREL`(의류), `FOOD_SERVICE`(외식). 브랜드가 정하고 바뀌지 않는다 |
| 브랜드 | Brand | 다이소, 지오다노, OO식당. 상품 마스터와 매장을 묶는 단위 (테넌트) |
| 매장 | Store | 브랜드의 점포 1곳. 업종을 브랜드에서 복사해 둔다 |
| 매장 직원 | StoreStaff | 회원이 어느 매장에서 어떤 역할(점주·매니저·직원)로 일하는지 |
| 상품 | Product | 브랜드 단위 상품 마스터. 의류에서는 "스타일" (예: 크루넥 티셔츠) |
| SKU | Sku | 판매·재고의 최소 단위. 의류는 스타일×색상×사이즈 하나, 생활용품·외식은 상품당 하나 |
| 상품 종류 | ProductKind | `MERCHANDISE`(판매 상품), `MENU`(메뉴), `INGREDIENT`(식자재) |
| 레시피 | Recipe | 메뉴 1개에 드는 식자재와 양 |
| 재고 | Stock | 매장×SKU 의 현재 수량 |
| 안전재고 | reorderPoint | 이 수량 이하로 내려가면 다시 채울 대상. 0 이면 미설정 |
| 매장 간 이동 | StockTransfer | 같은 브랜드 매장끼리 재고를 옮기는 1건. 받을 매장이 요청하고 보낼 매장이 승인·출고한다 |
| 재고 변동 | StockMovement | 재고가 바뀐 1건의 기록(원장). 추가만 한다 |
| 차감 정책 | StockDeductionPolicy | 판매한 SKU 가 어떤 재고를 얼마나 줄이는지 정하는 업종별 확장점 |
| 주문 | Order | 계산대 1건 또는 테이블·포장 1건. 결제까지의 단위 |
| 주문 방식 | OrderType | `COUNTER`(계산대), `DINE_IN`(매장 식사), `TAKEOUT`(포장) |

---

## 3. 업종 차이와 확장점

### 3.1 세 업종 비교

| | 생활용품 (다이소) | 의류 (지오다노) | 외식 (음식점) |
|---|---|---|---|
| 상품 종류 | 판매 상품 | 판매 상품 | 메뉴, 식자재 |
| 상품당 SKU | 1개 | 색상 × 사이즈 여러 개 | 1개 |
| SKU 옵션 | 없음 | 색상·사이즈 **필수**, 중복 금지 | 없음 |
| 바코드 | 있음 (판매 키) | 선택 | 없음 |
| 주문 방식 | 계산대 | 계산대 | 매장 식사(테이블 필수), 포장 |
| 판매 시 줄어드는 재고 | 판 SKU | 판 SKU | 레시피의 식자재 |
| 재고 단위 | 개 | 개 | 개, g, ml |
| 매장 간 이동 | 가끔 (품절 상품) | **자주** (사이즈 맞추기) | 가끔 (식자재 급히 빌리기) |

### 3.2 차이를 담는 곳 (이 세 군데 밖에서 업종으로 분기하지 않는다)

| 차이 | 담는 곳 | 방식 |
|---|---|---|
| 만들 수 있는 상품 종류 | `ProductKind.isAllowedFor(BusinessType)` | enum 규칙 (데이터) |
| SKU 옵션 규칙 | `BusinessType.requiresApparelOption()` + `Product.addSku` | enum 규칙 + 애그리거트 불변식 |
| 받는 주문 방식 | `OrderType.isAllowedFor(BusinessType)` | enum 규칙 |
| 판매가 줄이는 재고 | `StockDeductionPolicy` | **확장점: 인터페이스 하나 + 업종당 빈 하나** |

새 업종(예: 편의점)을 더할 때 할 일은 다음과 같다. ① `BusinessType` 값 추가 ② 위 두 `switch` 에 한 줄씩 추가 (switch 식이라 빠뜨리면 컴파일 에러) ③ 차감 정책 빈 하나 추가 (빠뜨리면 기동 실패, `StockDeductionPolicies`).

### 3.3 컨텍스트와 의존 방향

| 컨텍스트 / 패키지 | 책임 | 비고 |
|---|---|---|
| `member` | 가입, 로그인, 플랫폼 권한(USER / ADMIN / SYSTEM) | 구현 ([authentication-security.md](./authentication-security.md)) |
| `store` | 브랜드, 매장 생애주기, 매장 직원 역할 | 다른 컨텍스트가 업종(`BusinessType`)을 여기서 가져간다 |
| `catalog` | 상품, SKU, 레시피 | `store` 에 의존 (업종) |
| `inventory` | 재고 수량, 변동 원장, 차감 정책 | `store`(업종), `catalog`(레시피) 에 의존 |
| `order` | 주문, 줄, 결제 상태 | `store`, `catalog`(Sku) 에 의존. 재고는 모른다 |
| `order.application` (server) | 결제·환불 + 재고 변동을 한 트랜잭션으로 | `order` 와 `inventory` 를 조립하는 유일한 곳 |

```
store  <──  catalog  <──  inventory
  ^            ^              ^
  └──── order ─┘              │
          ^                   │
          └── OrderPaymentService (server) ──┘
```

- 애그리거트끼리는 **ID 로만 참조**한다 (`Store.brandId`, `Stock.skuId`, `OrderLine.skuId`). 같은 애그리거트 안(`Product`→`Sku`, `Order`→`OrderLine`, `Recipe`→`RecipeLine`)만 객체로 잇는다.
- 순환 의존이 없다. `order.domain` 은 `inventory` 를 import 하지 않는다.

---

## 4. 판매 흐름과 상태

### 4.1 업종별 판매 흐름

| 업종 | 흐름 |
|---|---|
| 다이소 | 계산대에서 주문 시작(`COUNTER`) → 바코드 스캔마다 `addItem` (같은 SKU 는 수량 합침) → 결제 → 판 SKU 차감 |
| 지오다노 | 계산대 주문 → 스타일·색상·사이즈 SKU 담기 (영수증 이름 "크루넥 티셔츠 (BLACK / L)") → 결제 → 판 SKU 차감. 매장에 없는 사이즈는 POS 에서 바로 다른 매장 재고를 찾아 이동 요청 → 상대 매장 승인·출고 → 우리 매장 입고 (§5.6) |
| 음식점 | 테이블 주문(`DINE_IN`, 테이블 번호 필수) 또는 포장(`TAKEOUT`) → 메뉴 추가 주문은 OPEN 동안 계속 → 결제 → 레시피대로 식자재 차감 |

### 4.2 주문 상태 머신

```
          addItem / changeQuantity / removeItem
               ┌──────┐
               v      │
  open() ──> OPEN ────┘
               │ \
          pay()│  \cancel()
               v   v
             PAID  CANCELLED
               │
       refund()│
               v
           REFUNDED
```

| 전이 | 조건 | 재고 |
|---|---|---|
| `open` | 매장이 영업 중(`OPEN`), 업종이 허용하는 주문 방식, 매장 식사만 테이블 번호 | - |
| `pay` | OPEN, 줄이 하나 이상, **받은 금액 == 합계** | `OrderPaymentService` 가 같은 트랜잭션에서 차감. 하나라도 모자라면 전부 롤백 |
| `cancel` | OPEN (결제 전) | 건드린 적이 없으니 되돌릴 것도 없다 |
| `refund` | PAID (전액 환불만) | 결제 때와 같은 정책으로 같은 양을 되돌린다 (`SALE_CANCEL`) |

### 4.3 매장 상태

`PREPARING → OPEN ⇄ CLOSED`. 주문은 `OPEN` 에서만 받는다.

---

## 5. 애그리거트

### 5.1 Brand · BrandAdmin · Store · StoreStaff (`store.domain`) **(구현)**

```
Brand
 ├─ id, name (UK)
 └─ businessType          // 바뀌지 않는다 (updatable=false)

BrandAdmin                // 본사. 브랜드를 만든 사람이 첫 관리자
 ├─ id, brandId, memberId (UK: brandId+memberId)

Store
 ├─ id, brandId, code (UK: brandId+code), name
 ├─ businessType          // 브랜드에서 복사. 주문·재고 규칙이 브랜드를 다시 읽지 않게
 └─ status: PREPARING | OPEN | CLOSED

StoreStaff
 ├─ id, storeId, memberId (UK: storeId+memberId)
 └─ role: OWNER | MANAGER | STAFF
```

- 플랫폼 권한(`RoleType`: 운영자)과 매장 역할(`StaffRole`: 점주·매니저·직원)은 다르다. 한 회원이 여러 매장에서 다른 역할을 가질 수 있다.
- 재고 수량을 직접 바꾸는 일(입고·실사·이동)은 `StaffRole.canManageInventory()` (점주·매니저). 판매는 모든 직원.
- 권한은 세 층이다.

| 층 | 누가 | 할 수 있는 일 | 인가 |
|---|---|---|---|
| 플랫폼 | `RoleType.ADMIN`, `SYSTEM` | 모든 브랜드·매장 | `Member.isAdmin()` |
| 본사 | `BrandAdmin` | 그 브랜드의 매장 등록·영업 상태·직원 배정, 상품·가격·레시피, 그리고 모든 매장 운영(재고 권한 포함) | `BrandAccessGuard`, `StoreAccessGuard` |
| 매장 | `StoreStaff` (점주·매니저·직원) | 배정된 매장 운영. 재고 변경은 점주·매니저 | `StoreAccessGuard` |

- **셀프 온보딩**: 로그인한 누구나 브랜드를 만들 수 있고 만든 사람이 브랜드 관리자가 된다. 가입 → 브랜드 → 매장 → 상품 → 판매가 한 사람 손으로 끝난다 ([product-strategy.md](./product-strategy.md) §5).
- 직원 배정은 가입한 회원의 이메일로만 한다. 초대 메일(아직 가입하지 않은 사람)은 다음 단계 `[ ]`.
- 브랜드 이름은 플랫폼 전체에서 하나다 (UK). 다른 고객사와 이름이 겹치면 지금은 409 `[ ]`.

### 5.2 Product · Sku (`catalog.domain`) **(구현)**

```
Product (루트)
 ├─ id, brandId, businessType, kind: MERCHANDISE | MENU | INGREDIENT
 ├─ name, category
 ├─ status: ON_SALE | SUSPENDED | DISCONTINUED
 └─ skus: List<Sku>

Sku
 ├─ id, product, brandId (UK: brandId+skuCode, brandId+barcode)
 ├─ skuCode, barcode
 ├─ price: Money          // 식자재는 0
 ├─ option: ApparelOption(color, size)   // 의류만, 그 밖에는 null
 └─ unit: EA | GRAM | MILLILITER
```

**불변식 (`Product.addSku` 한 곳에서 지킨다)**
- 업종이 허용하는 종류만 만든다 (리테일: 판매 상품, 외식: 메뉴·식자재).
- 의류: 옵션 필수, 같은 색상·사이즈 조합은 한 번만 (대소문자·공백 무시).
- 생활용품·외식: 옵션 없음, 상품당 SKU 하나.
- 식자재는 팔지 않는다: 가격 0, `changePrice` 거부, 주문에 담을 수 없다 (`Sku.isSellable()`).
- 단종(`DISCONTINUED`)은 되돌리지 않고 SKU 도 더할 수 없다.

**식자재를 SKU 로 둔 이유**: 재고 모델(`Stock`, `StockMovement`)을 업종과 상관없이 하나로 쓰기 위해서다. 외식에서 "파는 것(메뉴)"과 "쌓아 두는 것(식자재)"이 다를 뿐, 둘 다 SKU 이고 재고는 늘 SKU 에 붙는다.

### 5.3 Recipe (`catalog.domain`) **(구현)**

```
Recipe
 ├─ id, menuSkuId (UK)
 └─ lines: List<RecipeLine(ingredientSkuId, quantity)>   // 식자재 SKU 단위 기준 양
```

- 메뉴 SKU 에만, 재료는 식자재 SKU 만, 양은 1 이상, 같은 식자재 두 번 금지, 빈 레시피 금지.
- 바꿀 때는 통째로 바꾼다 (`replaceLines`). 이미 결제된 주문의 차감량은 바뀌지 않는다. 환불은 **지금 레시피**로 되돌린다 `[결정 필요: 레시피를 바꾼 뒤 이전 주문을 환불하면 결제 당시 양으로 되돌려야 하는가. 그렇다면 차감 내역을 주문에 남긴다]`.

### 5.4 Stock · StockMovement (`inventory.domain`) **(구현)**

```
Stock
 ├─ id, storeId, skuId (UK: storeId+skuId)
 ├─ quantity              // SKU 단위 정수, 0 미만 불가
 └─ version (@Version)    // 동시 판매 충돌 감지

StockMovement (원장, 추가만)
 ├─ id, stockId, storeId, skuId
 ├─ type: RECEIVE | SALE | SALE_CANCEL | ADJUST | TRANSFER_OUT | TRANSFER_IN
 ├─ delta (+/-), balanceAfter
 └─ reference: StockReference(type: ORDER | TRANSFER | MANUAL, id)
```

**불변식: "수량 변경과 변동 기록은 함께 남는다"** — `Stock` 의 수량 변경 메서드(`receive`, `deductForSale`, `restoreForCancel`, `adjustTo`, `transferOut`, `transferIn`)는 모두 `StockMovement` 를 돌려주고, `StockMovement` 생성자는 패키지 전용이라 `Stock` 만 만든다. 이 규칙을 쪼개지 않으려고 원장 생성을 별도 서비스로 빼지 않았다 ([solid.md](./solid.md) S).
- 남은 수량보다 많이 빼면 `InsufficientStockException`. 수량과 기록 모두 바뀌지 않는다 (경계값: 남은 수량 == 뺄 수량은 통과해 0).
- 변동 수량은 1 이상. 실사(`adjustTo`)만 차이 0 을 기록할 수 있다 (실사했다는 사실을 남긴다).
- `balanceAfter` 가 있어 원장만으로 어느 시점의 재고든 다시 셀 수 있다.
- 저장 전 `Stock`(ID 없음)으로는 기록을 만들 수 없다. 서비스가 `Stock` 을 먼저 저장한다.
- 외식 식자재도 음수를 허용하지 않는다 (**결정**). 입고 등록이 늦어 결제가 막히면 입고·실사를 먼저 하라는 뜻으로 409 `OUT_OF_STOCK` 을 준다.
- 안전재고(`reorderPoint`)는 재고마다 둔다. `needsReorder()` = 안전재고 > 0 이고 수량 ≤ 안전재고 (경계: 같으면 보충 대상).

### 5.5 StockDeductionPolicy (`inventory.domain` 포트, `inventory.policy` 구현) **(구현)**

```java
public interface StockDeductionPolicy {
    BusinessType businessType();
    List<StockRequirement> requirementsFor(Long soldSkuId, long soldQuantity);
}
```

| 구현체 | 업종 | 규칙 |
|---|---|---|
| `DirectSkuDeduction` | 생활용품, 의류 (빈 2개) | 판 SKU × 판 수량 |
| `RecipeStockDeduction` | 외식 | 레시피 줄마다 식자재 × (양 × 판 수량). 레시피가 없으면 빈 목록 |

**규약 (구현체를 바꿔도 같다, [solid.md](./solid.md) L)**
- 줄일 재고가 없으면 예외가 아니라 **빈 목록**이다.
- 돌려준 목록에 같은 SKU 가 두 번 나오지 않는다.
- `StockDeductionPolicies` 가 기동할 때 업종마다 정책이 정확히 하나인지 확인한다.

### 5.6 StockTransfer (`inventory.domain`) **(구현)**

```
StockTransfer (루트)
 ├─ id, brandId, fromStoreId(보낼 매장), toStoreId(받을 매장)
 ├─ status: REQUESTED | APPROVED | REJECTED | SHIPPED | RECEIVED | CANCELLED
 ├─ lines: List<TransferLine(skuId, itemName, quantity)>
 ├─ memo, rejectReason, requestedBy
 ├─ requestedAt, approvedAt, shippedAt, receivedAt, closedAt
 └─ version (@Version)
```

```
REQUESTED --approve(보낼)--> APPROVED --ship(보낼)--> SHIPPED --receive(받을)--> RECEIVED
    |  \                         |
    |   reject(보낼, 사유)         cancel(받을)
    v     v                      v
 CANCELLED REJECTED           CANCELLED
  (cancel, 받을)
```

- **누가 무엇을 할 수 있는지를 `availableActions(viewerStoreId, canManageInventory)` 한 곳에서 정한다.** 상태 전이 메서드도 이 결과로 검사하고, API 응답의 `availableActions` 로 화면 버튼도 이것으로 그린다. 서버 인가와 화면 버튼이 어긋나지 않는다.
- 재고는 출고 때 보낼 매장에서(`TRANSFER_OUT`), 입고 때 받을 매장에서(`TRANSFER_IN`) 바뀐다. 승인만으로는 재고를 잡아 두지 않는다 `[ ]` (예약 재고는 다음 단계).
- 출고할 때 보낼 매장 재고가 모자라면 409 `OUT_OF_STOCK`, 상태는 APPROVED 그대로 (롤백).
- 받은 수량이 보낸 수량과 다르면(파손·분실) 지금은 입고 뒤 실사로 맞춘다 `[ ]` (입고 수량 입력은 다음 단계).
- 같은 브랜드, 다른 매장, 재고를 세는 SKU(메뉴 제외), SKU 당 한 줄만.

### 5.7 Order · OrderLine (`order.domain`) **(구현)**

```
Order (루트, 테이블명 orders: ORDER 는 예약어)
 ├─ id, storeId, brandId, businessType
 ├─ orderType: COUNTER | DINE_IN | TAKEOUT, tableLabel
 ├─ status: OPEN | PAID | CANCELLED | REFUNDED
 ├─ lines: List<OrderLine>
 ├─ totalAmount: Money    // 줄이 바뀔 때마다 다시 계산
 ├─ paymentMethod: CARD | CASH | EASY_PAY, paidAt
 └─ version (@Version)

OrderLine
 ├─ id, order, skuId
 ├─ itemName, unitPrice   // 담은 순간의 값 (스냅샷)
 └─ quantity
```

**불변식 (`Order` 한 곳에서 지킨다)**
- 같은 브랜드의, 팔 수 있는 SKU 만 담는다 (판매 중지·단종·식자재 거부).
- 같은 SKU 를 다시 담으면 줄을 늘리지 않고 수량을 더한다 (바코드 두 번 스캔).
- 합계 = Σ(단가 × 수량). 줄이 바뀔 때마다 다시 계산해 저장한다.
- 결제 금액은 합계와 같아야 한다 (부분 결제 없음).
- 이름·단가는 스냅샷이라 나중에 상품 가격이 바뀌어도 영수증은 그대로다.

### 5.8 Money (`domain.base`) **(구현)**

원 단위 `long`. 음수 불가, 덧셈·곱셈은 overflow 때 예외(`Math.addExact`). 할인·환불은 음수 금액이 아니라 상태로 표현한다.

---

## 6. 결제와 재고의 트랜잭션 (`OrderPaymentService`)

```
@Transactional pay(orderId, method, paidAmount)
  1. order.pay(...)                        // 상태·금액 검증
  2. 정책 = policies.of(order.businessType)
  3. 줄마다 정책.requirementsFor → 같은 SKU 합치기
  4. SKU 마다 Stock 조회 → deductForSale → StockMovement 저장
     (재고 행이 없으면 0 으로 보고 InsufficientStockException)
  -> 하나라도 실패하면 주문 상태·재고·원장 전부 롤백
```

- 재고는 **결제 확정 때** 뺀다. 담는 동안(OPEN)에는 빼지 않는다 (장바구니·테이블 주문이 오래 열려 있어도 재고가 묶이지 않게).
- 동시에 같은 재고를 빼면 `@Version` 으로 한쪽이 `OptimisticLockingFailureException` → 409 `CONCURRENT_UPDATE`. 클라이언트가 다시 시도한다 `[측정 필요: 다이소 피크 시간 같은 SKU 동시 결제 빈도. 잦으면 비관적 락(SELECT ... FOR UPDATE)으로 바꾼다]`.
- 결제 승인(PG·카드 단말기)은 이 트랜잭션 밖에서 끝난 결과만 받는다. 외부 호출을 트랜잭션 안에 넣지 않는다.

---

## 7. 동시성

| 대상 | 방식 | 이유 |
|---|---|---|
| `Stock` | `@Version` 낙관적 락 | 대부분의 SKU 는 동시 판매가 드물다. 충돌하면 409 후 재시도 |
| `Order` | `@Version` 낙관적 락 | 같은 테이블 주문을 두 단말이 동시에 고치는 경우 |
| `Stock` 생성 | UK(store_id, sku_id) | 첫 입고가 동시에 와도 한 행만 |

---

## 8. 데이터 모델

### 8.1 테이블

| 테이블 | 주요 칼럼 | 인덱스/제약 |
|---|---|---|
| `member` | id, email, password, role_type, token_version, created_at, modified_at | UK(email) |
| `brand` | id, name, business_type | UK(name) |
| `store` | id, brand_id, business_type, code, name, status | UK(brand_id, code) |
| `store_staff` | id, store_id, member_id, role | UK(store_id, member_id) |
| `product` | id, brand_id, business_type, kind, name, category, status | |
| `sku` | id, product_id, brand_id, sku_code, barcode, price, option_color, option_size, unit | UK(brand_id, sku_code), UK(brand_id, barcode) |
| `recipe` | id, menu_sku_id | UK(menu_sku_id) |
| `recipe_line` | recipe_id, ingredient_sku_id, quantity | |
| `stock` | id, store_id, sku_id, quantity, version | UK(store_id, sku_id) |
| `stock_movement` | id, stock_id, store_id, sku_id, type, delta, balance_after, reference_type, reference_id, created_at | IDX(stock_id, id) |
| `orders` | id, store_id, brand_id, business_type, order_type, table_label, status, total_amount, payment_method, paid_at, version | `[ ]` IDX(store_id, paid_at) 매출 조회가 정해지면 |
| `order_line` | id, order_id, sku_id, item_name, unit_price, quantity | |

- 모든 시각은 UTC(`Instant`). 스키마는 초기에는 ddl-auto, 운영 전에 Flyway 로 옮긴다 `[ ]`.
- enum 칼럼은 varchar (`NoColumnCheckMySQLDialect`, [develop-principal.md](./develop-principal.md) §5).
- 구현 위치: `common/core/src/main/java/com/marinbom2/retailfnbplatform/{store,catalog,inventory,order}/domain`.

---

## 9. 화면과 요구사항 ID

코드 주석·라우트 이름에 그대로 쓴다 ([develop-principal.md](./develop-principal.md) §3).

### 9.1 화면 (SCR)

| ID | 화면 | 상태 |
|---|---|---|
| SCR-01 | 랜딩 (제품 소개, 데모 체험) | 구현 |
| SCR-02 | 로그인·가입 | 구현 |
| SCR-03 | 매장 선택 | 구현 |
| SCR-04 | 매장 대시보드 | 구현 |
| SCR-05 | 판매 POS (생활용품: 바코드 / 의류: 색상·사이즈 / 외식: 테이블·포장) | 구현 |
| SCR-06 | 주문 내역 (취소·환불) | 구현 |
| SCR-07 | 매장 재고 (입고·실사·안전재고·다른 매장 재고·이력) | 구현 |
| SCR-08 | 매장 간 이동 | 구현 |
| SCR-09 | 상품 (카탈로그) | 구현 (조회만) |
| SCR-10 | 본사: 내 브랜드·브랜드 만들기 | 구현 |
| SCR-11 | 본사: 매장 관리 (등록·영업 상태·직원) | 구현 |
| SCR-12 | 본사: 상품 관리 (업종별 등록, 판매 상태, 가격, 레시피) | 구현 |

### 9.2 요구사항 (FR)

| ID | 요구사항 | 상태 |
|---|---|---|
| FR-AU-001 | 이메일·비밀번호 가입 | 구현 |
| FR-AU-002 | 로그인·로그아웃 (JWT HttpOnly 쿠키), 내 정보 | 구현 |
| FR-AU-003 | 플랫폼 권한 (관리자 API) | 구현 |
| FR-ST-001 | 브랜드 만들기 (업종 지정, 만든 사람이 관리자) | 구현 |
| FR-ST-002 | 매장 생성·이름 변경·영업 시작·종료 | 구현 |
| FR-ST-003 | 매장 직원 배정·역할 변경·해제, 내 매장 목록 | 구현 |
| FR-CA-001 | 상품·SKU 등록 (업종별 옵션 규칙, 의류 색상×사이즈 일괄) | 구현 |
| FR-CA-002 | 판매 중지·재개·단종 | 구현 |
| FR-CA-003 | 메뉴 레시피 등록·변경 | 구현 |
| FR-CA-004 | SKU 가격 변경 (브랜드 단일가) | 구현 |
| FR-IN-001 | 입고 | 도메인 구현 |
| FR-IN-002 | 실사 조정 | 도메인 구현 |
| FR-IN-003 | 매장 간 이동 (요청 → 승인 → 출고 → 입고, 거절·취소) | 구현 |
| FR-IN-004 | 매장 재고·재고 변동 이력 조회 | 구현 |
| FR-IN-005 | 안전재고 설정 | 구현 |
| FR-IN-006 | 다른 매장 재고 찾기 | 구현 |
| FR-DA-001 | 오늘 매장 현황 (대시보드) | 구현 |
| FR-OR-001 | 주문 시작·상품 담기 | 도메인 구현 |
| FR-OR-002 | 결제 + 재고 차감 | 구현 |
| FR-OR-003 | 결제 전 취소 | 도메인 구현 |
| FR-OR-004 | 전액 환불 + 재고 복원 | 구현 |
| FR-OR-005 | 주문 목록·상세 | 구현 |

---

## 10. API

요청·응답 JSON 은 [api.md](./api.md) 가 기준이다. 매장 API 는 모두 `/api/v1/stores/{storeId}/...` 이고, `StoreAccessGuard` 가 그 매장 직원인지(재고를 바꾸는 일은 점주·매니저인지) 먼저 확인한다. 플랫폼 관리자는 모든 매장을 통과한다.

**오류 코드**

| 상황 | code | 상태 |
|---|---|---|
| 재고 부족 (`skuId` 확장 필드) | `OUT_OF_STOCK` | 409 |
| 주문 상태에서 못 하는 일 | `ORDER_STATE_CONFLICT` | 409 |
| 이동 상태·매장 쪽에서 못 하는 일 | `TRANSFER_STATE_CONFLICT` | 409 |
| 매장이 영업 중이 아님 | `STORE_STATE_CONFLICT` | 409 |
| 동시 수정 충돌 | `CONCURRENT_UPDATE` | 409 |
| 이 매장 직원이 아님, 재고 권한 없음 | `FORBIDDEN` | 403 |
| 그 밖의 도메인 규칙 위반 (업종에 없는 주문 방식, 결제 금액 불일치 등) | `DOMAIN_RULE_VIOLATION` | 400 |

---

## 11. 패키지 구조

| 모듈 | 패키지 |
|---|---|
| `common:core` | `store.domain`, `catalog.domain`, `inventory.domain`, `inventory.policy`, `order.domain`, `member.domain`, `domain.base`(BaseEntity, Money, DomainException), `type.common`(DescriptionCode, ErrorCode), `exception`(BusinessException), `config`, 공통 yml |
| `server` | 컨텍스트마다 `*.application`(유스케이스·트랜잭션) + `*.api`(컨트롤러·DTO): `store`(StoreAccessGuard, 내 매장), `catalog`, `inventory`(재고·이동), `order`(주문·결제), `dashboard`. 그 밖에 `member`, `security`, `exception`, `config`, `web`(SPA 포워딩), `demo`(demo 프로필 데이터) |
| `batch` | `batch.*` `[ ]` (일 마감 집계 등) |

```
com.marinbom2.retailfnbplatform
 +- store.domain        # Brand, Store, StoreStaff, BusinessType, StoreStatus, StaffRole
 +- catalog.domain      # Product, Sku, ApparelOption, Recipe, RecipeLine, ProductKind, ProductStatus, StockUnit
 +- inventory
 |   +- domain          # Stock, StockMovement, StockReference, MovementType, StockRequirement,
 |   |                  #   포트: StockDeductionPolicy
 |   +- policy          # DirectSkuDeduction, RecipeStockDeduction, StockDeductionPolicies, InventoryPolicyConfig
 +- order
 |   +- domain          # Order, OrderLine, OrderType, OrderStatus, PaymentMethod
 |   +- application     # OrderPaymentService (server)
 +- member              # domain / application / api
 +- security            # 필터 체인, JWT, 쿠키 (server)
```

---

## 12. 테스트

| 테스트 | 보는 것 |
|---|---|
| `StoreTest` | 매장 상태 전이, 업종 복사, 역할별 재고 권한 |
| `ProductTest` | 업종별 상품 종류, 의류 옵션 필수·중복 금지, 단일 SKU, 식자재 판매 불가, 단종 |
| `RecipeTest` | 메뉴·식자재 구분, 양 경계(0), 중복·빈 레시피 |
| `StockTest` | 원장 잔량, **경계값(남은 수량 == 뺄 수량 / +1)**, 실사, 매장 간 이동 |
| `StockDeductionPolicyTest` | 업종별 차감량, 레시피 없음 = 빈 목록, 업종당 정책 하나 |
| `OrderTest` | 업종별 주문 방식·테이블, 수량 합치기, 합계, 스냅샷, 결제 금액 경계(합계 −1원), 상태 전이 |
| `OrderPaymentServiceTest` | 다이소 차감·환불 복원, 음식점 레시피 차감·식자재 합치기, 재고 부족 |
| `StockTransferTest` | 이동 단계와 시각, 매장 쪽 규칙(받을 매장은 승인·출고 불가 등), 거절 사유, 가능한 동작, 요청 규칙 |
| `DemoScenarioTest` (server, H2) | demo 데이터로 실제 API: 대시보드, 품절 → 다른 매장 재고 → 이동 요청·승인·출고·입고 → 판매, 외식 테이블 결제와 식자재 차감, 직원·다른 매장 403, 의류 카탈로그 |

픽스처는 `common/core/src/testFixtures` 의 `DomainFixtures` (server 테스트도 같이 쓴다).

---

## 13. 비기능·보안

- 매장 데이터는 그 매장 직원(`StoreStaff`)과 플랫폼 운영자(`ADMIN`, `SYSTEM`)만 본다 `[ ]` (인가 구현은 API 를 만들 때).
- 로그에 결제 금액·카드 정보를 남기지 않는다. 주문 ID·매장 ID 만 남긴다.
- 금액은 `long` 원 단위. 부동소수점을 쓰지 않는다.

## 14. 미결 질문

| # | 질문 | 영향 |
|---|---|---|
| 1 | ~~매장별 판매가~~ → **브랜드 단일가로 결정** | - |
| 2 | 분류 체계: 자유 문자열(`category`)로 충분한가, 트리가 필요한가 | `Category` 애그리거트 |
| 3 | ~~외식 식자재 음수 재고~~ → **허용하지 않음으로 결정** | - |
| 4 | 레시피 변경 뒤 이전 주문 환불 시 차감량 | §5.3, 주문별 차감 내역 저장 |
| 5 | ~~매장 간 이동 흐름~~ → **요청 → 승인 → 출고 → 입고로 결정, 구현** (§5.6) | - |
| 5-1 | 승인 때 보낼 매장 재고를 예약해 둘지 (승인 뒤 팔려 출고가 막히는 경우) | 예약 재고(`reserved`) |
| 5-2 | 입고 수량을 받은 대로 입력할지 (파손·분실) | `TransferLine.receivedQuantity` |
| 6 | 주방 진행 상태(조리 중·서빙 완료)를 주문 줄에 둘지 | `OrderLine.kitchenStatus` |
| 7 | 테이블을 엔티티로 관리할지 (배치도·합석) | `DiningTable` 애그리거트 |
| 8 | 부분 결제·더치페이·복합 결제 | `Payment` 를 Order 밖 애그리거트로 |
| 9 | 대시보드 집계를 주문 원본에서 매번 계산 → 매장당 하루 주문이 많아지면 일 마감 집계 테이블 | `batch` 일 마감, `daily_sales` |
