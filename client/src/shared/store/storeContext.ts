import { createContext, use } from 'react'
import type { MyStore } from '../api/store/storeService'

export type StoreContextValue = {
  /** 지금 보고 있는 매장 (경로의 storeId) */
  store: MyStore
  /** 내가 일하는 매장 전부 (매장 바꾸기, 이동 요청 대상 표시) */
  stores: MyStore[]
}

export const StoreContext = createContext<StoreContextValue | null>(null)

export function useCurrentStore() {
  const context = use(StoreContext)
  if (!context) throw new Error('useCurrentStore 는 매장 화면 안에서만 쓸 수 있습니다.')
  return context
}

export const isFoodService = (store: MyStore) => store.businessType.code === 'FOOD_SERVICE'
export const isApparel = (store: MyStore) => store.businessType.code === 'APPAREL'
