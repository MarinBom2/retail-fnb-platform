import { apiRequest } from '../../core/apiClient'
import type { CodeLabel } from '../../types/common'

export interface CatalogSku {
  skuId: number
  skuCode: string
  barcode: string | null
  displayName: string
  color: string | null
  size: string | null
  price: number
  unit: CodeLabel
  sellable: boolean
  stockTracked: boolean
  stockQuantity: number | null
}

/** 서버 CatalogProductResponse (FR-CA-001) */
export interface CatalogProduct {
  productId: number
  name: string
  category: string | null
  kind: CodeLabel
  status: CodeLabel
  skus: CatalogSku[]
}

export const catalogApi = {
  list: (storeId: number) => apiRequest<CatalogProduct[]>(`/api/v1/stores/${storeId}/catalog`),
}
