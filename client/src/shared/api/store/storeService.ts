import { apiRequest } from '../../core/apiClient'
import type { CodeLabel } from '../../types/common'

/** 서버 MyStoreResponse (GET /api/v1/me/stores) */
export interface MyStore {
  storeId: number
  storeCode: string
  storeName: string
  status: CodeLabel
  brandId: number
  brandName: string
  businessType: CodeLabel
  role: CodeLabel
  canManageInventory: boolean
}

export const storeApi = {
  myStores: () => apiRequest<MyStore[]>('/api/v1/me/stores'),
}
