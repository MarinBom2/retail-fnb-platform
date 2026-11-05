import { createContext, use } from 'react'

export type TransferPrefill = {
  skuId?: number
  fromStoreId?: number
  quantity?: number
}

export type AvailabilityRequest = {
  skuId: number
  itemName?: string
  /** 품절로 열었을 때 맨 위에 보여 줄 문장 */
  reason?: string
}

export type StockActions = {
  /** 이동 요청 대화상자 (어디서나 같은 것) */
  openTransferRequest: (prefill?: TransferPrefill) => void
  /** 다른 매장 재고 보기 → 이동 요청 */
  openAvailability: (request: AvailabilityRequest) => void
  /** 이동을 새로 요청할 때마다 1씩 오른다. 이동 목록이 다시 읽는 데 쓴다 */
  transferVersion: number
}

export const StockActionsContext = createContext<StockActions | null>(null)

export function useStockActions() {
  const context = use(StockActionsContext)
  if (!context) throw new Error('useStockActions 는 매장 화면 안에서만 쓸 수 있습니다.')
  return context
}
