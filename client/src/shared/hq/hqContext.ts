import { createContext, use } from 'react'
import type { BrandSummary } from '../api/hq/hqService'

export type HqContextValue = {
  /** 지금 보고 있는 브랜드 (경로의 brandId) */
  brand: BrandSummary
  /** 내가 관리하는 브랜드 전부 (브랜드 바꾸기) */
  brands: BrandSummary[]
  /** 매장 수·상품 수가 바뀐 뒤 머리글 숫자를 다시 읽는다 */
  reloadBrands: () => void
}

export const HqContext = createContext<HqContextValue | null>(null)

export function useCurrentBrand() {
  const context = use(HqContext)
  if (!context) throw new Error('useCurrentBrand 는 본사 화면 안에서만 쓸 수 있습니다.')
  return context
}

export const isApparelBrand = (brand: BrandSummary) => brand.businessType.code === 'APPAREL'
export const isFoodBrand = (brand: BrandSummary) => brand.businessType.code === 'FOOD_SERVICE'
