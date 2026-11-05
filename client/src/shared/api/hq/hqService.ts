import { apiRequest } from '../../core/apiClient'
import type { CodeLabel } from '../../types/common'

/** 서버 BrandSummary (md/api.md §8.1) */
export interface BrandSummary {
  brandId: number
  name: string
  businessType: CodeLabel
  storeCount: number
  openStoreCount: number
  productCount: number
}

/** 서버 HqStore (§8.2) */
export interface HqStore {
  storeId: number
  code: string
  name: string
  status: CodeLabel
  staffCount: number
}

export interface StoreStaffItem {
  staffId: number
  memberId: number
  email: string
  role: CodeLabel
}

/** 서버 HqProduct (§8.3) */
export interface HqProduct {
  productId: number
  name: string
  category: string | null
  kind: CodeLabel
  status: CodeLabel
  skus: HqSku[]
}

export interface HqSku {
  skuId: number
  skuCode: string
  barcode: string | null
  displayName: string
  color: string | null
  size: string | null
  /** 식자재는 0 */
  price: number
  unit: CodeLabel
  hasRecipe: boolean
}

/** 등록 요청의 SKU 한 줄. 식자재는 price 를 보내지 않는다 */
export interface NewSku {
  skuCode: string
  barcode?: string | null
  price?: number | null
  color?: string | null
  size?: string | null
  /** EA | GRAM | MILLILITER */
  unit: string
}

export interface NewProduct {
  /** MERCHANDISE | MENU | INGREDIENT */
  kind: string
  name: string
  category?: string | null
  skus: NewSku[]
}

export interface RecipeResponse {
  menuSkuId: number
  lines: { ingredientSkuId: number; ingredientName: string; unit: CodeLabel; quantity: number }[]
}

export type BusinessTypeCode = 'GENERAL_MERCHANDISE' | 'APPAREL' | 'FOOD_SERVICE'
export type StaffRoleCode = 'OWNER' | 'MANAGER' | 'STAFF'
export type ProductStatusAction = 'suspend' | 'resume' | 'discontinue'

const json = (body: unknown) => JSON.stringify(body)
const brand = (brandId: number) => `/api/v1/brands/${brandId}`

export const hqApi = {
  // 8.1 브랜드
  brands: () => apiRequest<BrandSummary[]>('/api/v1/brands'),
  createBrand: (name: string, businessType: BusinessTypeCode) =>
    apiRequest<BrandSummary>('/api/v1/brands', { method: 'POST', body: json({ name, businessType }) }),
  brand: (brandId: number) => apiRequest<BrandSummary>(brand(brandId)),
  renameBrand: (brandId: number, name: string) =>
    apiRequest<BrandSummary>(brand(brandId), { method: 'PATCH', body: json({ name }) }),

  // 8.2 매장
  stores: (brandId: number) => apiRequest<HqStore[]>(`${brand(brandId)}/stores`),
  createStore: (brandId: number, code: string, name: string) =>
    apiRequest<HqStore>(`${brand(brandId)}/stores`, { method: 'POST', body: json({ code, name }) }),
  renameStore: (brandId: number, storeId: number, name: string) =>
    apiRequest<HqStore>(`${brand(brandId)}/stores/${storeId}`, { method: 'PATCH', body: json({ name }) }),
  openStore: (brandId: number, storeId: number) =>
    apiRequest<HqStore>(`${brand(brandId)}/stores/${storeId}/open`, { method: 'POST' }),
  closeStore: (brandId: number, storeId: number) =>
    apiRequest<HqStore>(`${brand(brandId)}/stores/${storeId}/close`, { method: 'POST' }),
  staff: (brandId: number, storeId: number) =>
    apiRequest<StoreStaffItem[]>(`${brand(brandId)}/stores/${storeId}/staff`),
  addStaff: (brandId: number, storeId: number, email: string, role: StaffRoleCode) =>
    apiRequest<StoreStaffItem>(`${brand(brandId)}/stores/${storeId}/staff`, { method: 'POST', body: json({ email, role }) }),
  changeStaffRole: (brandId: number, storeId: number, staffId: number, role: StaffRoleCode) =>
    apiRequest<StoreStaffItem>(`${brand(brandId)}/stores/${storeId}/staff/${staffId}`, {
      method: 'PATCH',
      body: json({ role }),
    }),
  removeStaff: (brandId: number, storeId: number, staffId: number) =>
    apiRequest<void>(`${brand(brandId)}/stores/${storeId}/staff/${staffId}`, { method: 'DELETE' }),

  // 8.3 상품
  products: (brandId: number) => apiRequest<HqProduct[]>(`${brand(brandId)}/products`),
  createProduct: (brandId: number, product: NewProduct) =>
    apiRequest<HqProduct>(`${brand(brandId)}/products`, { method: 'POST', body: json(product) }),
  addSku: (brandId: number, productId: number, sku: NewSku) =>
    apiRequest<HqProduct>(`${brand(brandId)}/products/${productId}/skus`, { method: 'POST', body: json(sku) }),
  changeProductStatus: (brandId: number, productId: number, action: ProductStatusAction) =>
    apiRequest<HqProduct>(`${brand(brandId)}/products/${productId}/${action}`, { method: 'POST' }),
  changePrice: (brandId: number, skuId: number, price: number) =>
    apiRequest<HqSku>(`${brand(brandId)}/skus/${skuId}/price`, { method: 'PATCH', body: json({ price }) }),
  recipe: (brandId: number, skuId: number) => apiRequest<RecipeResponse>(`${brand(brandId)}/skus/${skuId}/recipe`),
  saveRecipe: (brandId: number, skuId: number, lines: { ingredientSkuId: number; quantity: number }[]) =>
    apiRequest<RecipeResponse>(`${brand(brandId)}/skus/${skuId}/recipe`, { method: 'PUT', body: json({ lines }) }),
}
