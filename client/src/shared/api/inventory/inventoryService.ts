import { apiRequest } from '../../core/apiClient'
import type { CodeLabel } from '../../types/common'

/** 서버 StockItemResponse */
export interface StockItem {
  skuId: number
  productName: string
  itemName: string
  skuCode: string
  kind: CodeLabel
  unit: CodeLabel
  quantity: number
  reorderPoint: number
  needsReorder: boolean
}

/** 같은 브랜드의 다른 영업 중 매장 재고 (FR-IN-006) */
export interface StoreAvailability {
  storeId: number
  storeName: string
  quantity: number
}

export interface StockMovementItem {
  movementId: number
  type: CodeLabel
  delta: number
  balanceAfter: number
  referenceType: CodeLabel
  referenceId: number | null
  createdAt: string
}

const base = (storeId: number, skuId?: number) =>
  skuId === undefined ? `/api/v1/stores/${storeId}/stocks` : `/api/v1/stores/${storeId}/stocks/${skuId}`

export const inventoryApi = {
  list: (storeId: number) => apiRequest<StockItem[]>(base(storeId)),
  /** FR-IN-001 입고 */
  receive: (storeId: number, skuId: number, quantity: number) =>
    apiRequest<StockItem>(`${base(storeId, skuId)}/receive`, { method: 'POST', body: JSON.stringify({ quantity }) }),
  /** FR-IN-002 실사 */
  adjust: (storeId: number, skuId: number, countedQuantity: number) =>
    apiRequest<StockItem>(`${base(storeId, skuId)}/adjust`, { method: 'POST', body: JSON.stringify({ countedQuantity }) }),
  /** FR-IN-005 안전재고 */
  changeReorderPoint: (storeId: number, skuId: number, reorderPoint: number) =>
    apiRequest<StockItem>(`${base(storeId, skuId)}/reorder-point`, { method: 'PUT', body: JSON.stringify({ reorderPoint }) }),
  availability: (storeId: number, skuId: number) =>
    apiRequest<StoreAvailability[]>(`${base(storeId, skuId)}/availability`),
  movements: (storeId: number, skuId: number) =>
    apiRequest<StockMovementItem[]>(`${base(storeId, skuId)}/movements`),
}
