import { apiRequest } from '../../core/apiClient'
import type { CodeLabel } from '../../types/common'

export interface LowStockItem {
  skuId: number
  itemName: string
  quantity: number
  reorderPoint: number
  unit: CodeLabel
  bestSourceStoreId: number | null
  bestSourceStoreName: string | null
  bestSourceQuantity: number | null
}

/** 서버 DashboardResponse (FR-DA-001) */
export interface Dashboard {
  storeId: number
  businessDate: string
  salesAmount: number
  orderCount: number
  averageTicket: number
  refundedAmount: number
  hourlySales: { hour: number; amount: number }[]
  topItems: { skuId: number; itemName: string; quantity: number; amount: number }[]
  lowStock: LowStockItem[]
  openOrderCount: number
  transfersToApprove: number
  transfersToShip: number
  transfersToReceive: number
}

export const dashboardApi = {
  get: (storeId: number) => apiRequest<Dashboard>(`/api/v1/stores/${storeId}/dashboard`),
}
