# API 계약 — 매장 운영 (대시보드 · 카탈로그 · 재고 · 매장 간 이동 · 주문)

> 목적: 화면(client)과 서버가 같은 모양의 JSON 을 주고받도록 요청·응답을 고정한다. 바꿀 때는 이 문서부터 고친다.
> 공통 규칙: [develop-principal.md](./develop-principal.md) §7(오류 ProblemDetail)·§8(경로). 도메인 규칙: [domain-design.md](./domain-design.md).
> 인증은 HttpOnly 쿠키라 화면은 `credentials: 'include'` 만 붙인다 ([authentication-security.md](./authentication-security.md)).

## 0. 공통

| 항목 | 규칙 |
|---|---|
| 경로 | 매장 단위 API 는 모두 `/api/v1/stores/{storeId}/...`. 서버가 **이 매장의 직원인지** 확인한다 (아니면 403 `FORBIDDEN`). 플랫폼 관리자(`ADMIN`, `SYSTEM`)는 모든 매장 통과 |
| enum | `{ "code": "APPAREL", "description": "의류 리테일" }`. 비교는 `code`, 표시는 `description` |
| 금액 | 원 단위 정수 (`number`). 예: `19900` |
| 시각 | ISO-8601 UTC 문자열 (`"2026-11-05T03:00:00Z"`). 화면이 한국 시간으로 바꿔 보여 준다 |
| 날짜 | 영업일은 `Asia/Seoul` 기준 `"2026-11-05"` |
| 재고 권한 | 입고·실사·안전재고·이동 승인/출고/입고/요청은 `canManageInventory` (점주·매니저). 직원(STAFF)은 403 |

타입 표기 (TypeScript):

```ts
interface CodeLabel { code: string; description: string }
```

---

## 1. 내 매장

### GET `/api/v1/me/stores` — 내가 일하는 매장 목록 (FR-ST-003)

```ts
interface MyStore {
  storeId: number
  storeCode: string
  storeName: string
  status: CodeLabel            // PREPARING | OPEN | CLOSED
  brandId: number
  brandName: string
  businessType: CodeLabel      // GENERAL_MERCHANDISE | APPAREL | FOOD_SERVICE
  role: CodeLabel              // OWNER | MANAGER | STAFF
  canManageInventory: boolean
}
// 응답: MyStore[]  (브랜드 이름, 매장 이름 순)
```

---

## 2. 대시보드

### GET `/api/v1/stores/{storeId}/dashboard` — 오늘 매장 현황 (FR-DA-001)

```ts
interface Dashboard {
  storeId: number
  businessDate: string                 // "2026-11-05" (Asia/Seoul)
  salesAmount: number                  // 오늘 결제 완료 합계 (환불 제외)
  orderCount: number                   // 오늘 결제 완료 건수
  averageTicket: number                // salesAmount / orderCount (0 건이면 0)
  refundedAmount: number               // 오늘 결제됐다가 환불된 합계
  hourlySales: { hour: number; amount: number }[]   // 0~23 시 24칸, 한국 시간
  topItems: { skuId: number; itemName: string; quantity: number; amount: number }[]  // 수량 많은 순 최대 5
  lowStock: LowStockItem[]             // 안전재고 이하, 부족한 순 최대 10
  openOrderCount: number               // 결제 전 주문 (외식: 이용 중인 테이블)
  transfersToApprove: number           // 다른 매장이 우리에게 요청해 승인 기다리는 건
  transfersToShip: number              // 승인했고 아직 출고 안 한 건
  transfersToReceive: number           // 우리에게 출고돼 입고 기다리는 건
}

interface LowStockItem {
  skuId: number
  itemName: string
  quantity: number
  reorderPoint: number
  unit: CodeLabel                      // EA | GRAM | MILLILITER
  bestSourceStoreId: number | null     // 같은 브랜드에서 이 SKU 재고가 가장 많은 다른 영업 중 매장
  bestSourceStoreName: string | null
  bestSourceQuantity: number | null
}
```

---

## 3. 카탈로그

### GET `/api/v1/stores/{storeId}/catalog` — 브랜드 상품과 이 매장 재고 (FR-CA-001)

```ts
interface CatalogProduct {
  productId: number
  name: string
  category: string | null
  kind: CodeLabel                      // MERCHANDISE | MENU | INGREDIENT
  status: CodeLabel                    // ON_SALE | SUSPENDED | DISCONTINUED
  skus: CatalogSku[]
}

interface CatalogSku {
  skuId: number
  skuCode: string
  barcode: string | null
  displayName: string                  // 의류는 "크루넥 티셔츠 (BLACK / L)"
  color: string | null                 // 의류만
  size: string | null                  // 의류만
  price: number                        // 식자재는 0
  unit: CodeLabel
  sellable: boolean                    // POS 에 보여 줄지
  stockTracked: boolean                // 메뉴는 false (레시피로 식자재가 빠진다)
  stockQuantity: number | null         // stockTracked 가 false 면 null
}
// 응답: CatalogProduct[] (분류, 이름 순)
```

---

## 4. 재고

```ts
interface StockItem {
  skuId: number
  productName: string
  itemName: string                     // displayName
  skuCode: string
  kind: CodeLabel
  unit: CodeLabel
  quantity: number
  reorderPoint: number                 // 0 이면 안전재고 미설정
  needsReorder: boolean                // reorderPoint > 0 && quantity <= reorderPoint
}
```

| 메서드 | 경로 | 요청 | 응답 | FR |
|---|---|---|---|---|
| GET | `/api/v1/stores/{storeId}/stocks` | | `StockItem[]` 재고를 세는 SKU 전부(리테일: 판매 상품, 외식: 식자재). 입고 전이면 0 | FR-IN-004 |
| POST | `/api/v1/stores/{storeId}/stocks/{skuId}/receive` | `{ quantity: number }` (1 이상) | `StockItem` | FR-IN-001 |
| POST | `/api/v1/stores/{storeId}/stocks/{skuId}/adjust` | `{ countedQuantity: number }` (0 이상) | `StockItem` | FR-IN-002 |
| PUT | `/api/v1/stores/{storeId}/stocks/{skuId}/reorder-point` | `{ reorderPoint: number }` (0 이상) | `StockItem` | FR-IN-005 |
| GET | `/api/v1/stores/{storeId}/stocks/{skuId}/availability` | | `StoreAvailability[]` | FR-IN-006 |
| GET | `/api/v1/stores/{storeId}/stocks/{skuId}/movements` | | `StockMovementItem[]` 최근 50건, 최신 먼저 | FR-IN-004 |

```ts
/** 같은 브랜드의 다른 영업 중 매장 중 이 SKU 재고가 1 이상인 곳. 많은 순 */
interface StoreAvailability {
  storeId: number
  storeName: string
  quantity: number
}

interface StockMovementItem {
  movementId: number
  type: CodeLabel                      // RECEIVE | SALE | SALE_CANCEL | ADJUST | TRANSFER_OUT | TRANSFER_IN
  delta: number                        // + 입고, - 출고
  balanceAfter: number
  referenceType: CodeLabel             // ORDER | TRANSFER | MANUAL
  referenceId: number | null
  createdAt: string
}
```

---

## 5. 매장 간 이동 (요청 → 승인 → 출고 → 입고)

```
REQUESTED --approve--> APPROVED --ship--> SHIPPED --receive--> RECEIVED
    |  \                   |
    |   reject             cancel
    v     v                v
 CANCELLED REJECTED     CANCELLED
```

| 동작 | 누가 (해당 매장의 점주·매니저) | 재고 |
|---|---|---|
| 요청 `request` | **받을 매장** (재고가 필요한 쪽) | - |
| 승인 `approve` / 거절 `reject` | **보낼 매장** | - |
| 출고 `ship` | 보낼 매장 (APPROVED 일 때) | 보낼 매장 `TRANSFER_OUT`. 모자라면 409 `OUT_OF_STOCK` |
| 입고 `receive` | 받을 매장 (SHIPPED 일 때) | 받을 매장 `TRANSFER_IN` |
| 취소 `cancel` | 받을 매장 (REQUESTED, APPROVED 일 때) | - |

```ts
interface Transfer {
  transferId: number
  fromStoreId: number
  fromStoreName: string
  toStoreId: number
  toStoreName: string
  direction: 'INCOMING' | 'OUTGOING'   // 경로의 storeId 기준. 받는 쪽이면 INCOMING
  status: CodeLabel                    // REQUESTED | APPROVED | REJECTED | SHIPPED | RECEIVED | CANCELLED
  lines: { skuId: number; itemName: string; quantity: number }[]
  memo: string | null
  rejectReason: string | null
  requestedAt: string
  approvedAt: string | null
  shippedAt: string | null
  receivedAt: string | null
  closedAt: string | null              // 거절·취소 시각
  availableActions: TransferAction[]   // 경로의 매장 + 내 역할로 지금 할 수 있는 것
}
type TransferAction = 'APPROVE' | 'REJECT' | 'SHIP' | 'RECEIVE' | 'CANCEL'
```

| 메서드 | 경로 | 요청 | 응답 | FR |
|---|---|---|---|---|
| POST | `/api/v1/stores/{storeId}/transfers` | `{ fromStoreId: number, lines: {skuId, quantity}[], memo?: string }` storeId 가 받을 매장 | `Transfer` (201) | FR-IN-003 |
| GET | `/api/v1/stores/{storeId}/transfers` | | `Transfer[]` 보내고 받는 것 모두, 최신 50건 | FR-IN-003 |
| POST | `/api/v1/stores/{storeId}/transfers/{transferId}/approve` | | `Transfer` | FR-IN-003 |
| POST | `/api/v1/stores/{storeId}/transfers/{transferId}/reject` | `{ reason: string }` | `Transfer` | FR-IN-003 |
| POST | `/api/v1/stores/{storeId}/transfers/{transferId}/ship` | | `Transfer` | FR-IN-003 |
| POST | `/api/v1/stores/{storeId}/transfers/{transferId}/receive` | | `Transfer` | FR-IN-003 |
| POST | `/api/v1/stores/{storeId}/transfers/{transferId}/cancel` | | `Transfer` | FR-IN-003 |

오류: 상태에 맞지 않는 동작 409 `TRANSFER_STATE_CONFLICT`, 같은 매장·다른 브랜드·빈 줄·재고를 세지 않는 SKU 400 `DOMAIN_RULE_VIOLATION`, 출고 때 재고 부족 409 `OUT_OF_STOCK` (+`skuId`).

---

## 6. 주문 (POS)

```ts
interface Order {
  orderId: number
  storeId: number
  orderType: CodeLabel                 // COUNTER | DINE_IN | TAKEOUT
  tableLabel: string | null
  status: CodeLabel                    // OPEN | PAID | CANCELLED | REFUNDED
  lines: { skuId: number; itemName: string; unitPrice: number; quantity: number; lineAmount: number }[]
  totalAmount: number
  paymentMethod: CodeLabel | null      // CARD | CASH | EASY_PAY
  paidAt: string | null
  createdAt: string
}
```

| 메서드 | 경로 | 요청 | 응답 | FR |
|---|---|---|---|---|
| POST | `/api/v1/stores/{storeId}/orders` | `{ orderType: string, tableLabel?: string, items: {skuId, quantity}[] }` items 는 빈 배열 가능 | `Order` (201) | FR-OR-001 |
| GET | `/api/v1/stores/{storeId}/orders?status=OPEN` | status 생략하면 전체 | `Order[]` 최신 50건 | FR-OR-005 |
| GET | `/api/v1/stores/{storeId}/orders/{orderId}` | | `Order` | FR-OR-005 |
| POST | `/api/v1/stores/{storeId}/orders/{orderId}/items` | `{ skuId, quantity }` 같은 SKU 면 수량을 더한다 | `Order` | FR-OR-001 |
| PATCH | `/api/v1/stores/{storeId}/orders/{orderId}/items/{skuId}` | `{ quantity }` | `Order` | FR-OR-001 |
| DELETE | `/api/v1/stores/{storeId}/orders/{orderId}/items/{skuId}` | | `Order` | FR-OR-001 |
| POST | `/api/v1/stores/{storeId}/orders/{orderId}/payment` | `{ method: string, amount: number }` amount 는 합계와 같아야 함 | `Order` | FR-OR-002 |
| POST | `/api/v1/stores/{storeId}/orders/{orderId}/cancel` | | `Order` | FR-OR-003 |
| POST | `/api/v1/stores/{storeId}/orders/{orderId}/refund` | | `Order` | FR-OR-004 |

오류: 재고 부족 409 `OUT_OF_STOCK` + `skuId` (화면은 `availability` 로 다른 매장 재고를 보여 주고 이동 요청을 연다), 상태 409 `ORDER_STATE_CONFLICT`, 영업 중 아님 409 `STORE_STATE_CONFLICT`, 업종에 없는 주문 방식·결제 금액 불일치 400 `DOMAIN_RULE_VIOLATION`.

---

## 7. 데모 모드

`demo` 프로필로 서버를 띄우면 메모리 DB(H2)에 데모 데이터가 들어간다 (MySQL·Docker 불필요). 영업 시연·화면 개발용.

| 계정 | 비밀번호 | 매장 |
|---|---|---|
| `demo@retailfnb.dev` | `Demo1234!` | 데모 브랜드 3개의 모든 매장 점주 |
| `staff@retailfnb.dev` | `Demo1234!` | 생활용품 브랜드 1호점 직원 (재고 권한 없음) |

데모 브랜드는 실제 상호가 아닌 가상 이름이다: 생활용품 `데일리마트`, 의류 `핏앤코`, 외식 `온기식당`.
