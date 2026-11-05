import { apiRequest } from '../../core/apiClient'
import type { CodeLabel } from '../../types/common'

export type OrderTypeCode = 'COUNTER' | 'DINE_IN' | 'TAKEOUT'
export type OrderStatusCode = 'OPEN' | 'PAID' | 'CANCELLED' | 'REFUNDED'
export type PaymentMethodCode = 'CARD' | 'CASH' | 'EASY_PAY'

export interface OrderLine {
  skuId: number
  itemName: string
  unitPrice: number
  quantity: number
  lineAmount: number
}

/** 서버 OrderResponse */
export interface Order {
  orderId: number
  storeId: number
  orderType: CodeLabel
  tableLabel: string | null
  status: CodeLabel
  lines: OrderLine[]
  totalAmount: number
  paymentMethod: CodeLabel | null
  paidAt: string | null
  createdAt: string
}

export interface OrderCreateRequest {
  orderType: OrderTypeCode
  tableLabel?: string
  items: { skuId: number; quantity: number }[]
}

const base = (storeId: number) => `/api/v1/stores/${storeId}/orders`
const orderPath = (storeId: number, orderId: number) => `${base(storeId)}/${orderId}`

export const orderApi = {
  /** FR-OR-001 */
  create: (storeId: number, request: OrderCreateRequest) =>
    apiRequest<Order>(base(storeId), { method: 'POST', body: JSON.stringify(request) }),
  /** FR-OR-005. status 생략하면 전체 */
  list: (storeId: number, status?: OrderStatusCode) => apiRequest<Order[]>(base(storeId), { query: { status } }),
  get: (storeId: number, orderId: number) => apiRequest<Order>(orderPath(storeId, orderId)),
  addItem: (storeId: number, orderId: number, skuId: number, quantity: number) =>
    apiRequest<Order>(`${orderPath(storeId, orderId)}/items`, { method: 'POST', body: JSON.stringify({ skuId, quantity }) }),
  changeQuantity: (storeId: number, orderId: number, skuId: number, quantity: number) =>
    apiRequest<Order>(`${orderPath(storeId, orderId)}/items/${skuId}`, { method: 'PATCH', body: JSON.stringify({ quantity }) }),
  removeItem: (storeId: number, orderId: number, skuId: number) =>
    apiRequest<Order>(`${orderPath(storeId, orderId)}/items/${skuId}`, { method: 'DELETE' }),
  /** FR-OR-002. amount 는 합계와 같아야 한다 */
  pay: (storeId: number, orderId: number, method: PaymentMethodCode, amount: number) =>
    apiRequest<Order>(`${orderPath(storeId, orderId)}/payment`, { method: 'POST', body: JSON.stringify({ method, amount }) }),
  cancel: (storeId: number, orderId: number) => apiRequest<Order>(`${orderPath(storeId, orderId)}/cancel`, { method: 'POST' }),
  refund: (storeId: number, orderId: number) => apiRequest<Order>(`${orderPath(storeId, orderId)}/refund`, { method: 'POST' }),
}
