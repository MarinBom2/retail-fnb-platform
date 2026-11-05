import { apiRequest } from '../../core/apiClient'
import type { CodeLabel } from '../../types/common'

export type TransferAction = 'APPROVE' | 'REJECT' | 'SHIP' | 'RECEIVE' | 'CANCEL'

/** 서버 TransferResponse (FR-IN-003). direction·availableActions 는 경로의 매장 기준 */
export interface Transfer {
  transferId: number
  fromStoreId: number
  fromStoreName: string
  toStoreId: number
  toStoreName: string
  direction: 'INCOMING' | 'OUTGOING'
  status: CodeLabel
  lines: { skuId: number; itemName: string; quantity: number }[]
  memo: string | null
  rejectReason: string | null
  requestedAt: string
  approvedAt: string | null
  shippedAt: string | null
  receivedAt: string | null
  closedAt: string | null
  availableActions: TransferAction[]
}

export interface TransferCreateRequest {
  fromStoreId: number
  lines: { skuId: number; quantity: number }[]
  memo?: string
}

const base = (storeId: number) => `/api/v1/stores/${storeId}/transfers`

const action = (storeId: number, transferId: number, name: string, body?: unknown) =>
  apiRequest<Transfer>(`${base(storeId)}/${transferId}/${name}`, {
    method: 'POST',
    body: body === undefined ? undefined : JSON.stringify(body),
  })

export const transferApi = {
  /** storeId 가 받을 매장 */
  request: (storeId: number, request: TransferCreateRequest) =>
    apiRequest<Transfer>(base(storeId), { method: 'POST', body: JSON.stringify(request) }),
  list: (storeId: number) => apiRequest<Transfer[]>(base(storeId)),
  approve: (storeId: number, transferId: number) => action(storeId, transferId, 'approve'),
  reject: (storeId: number, transferId: number, reason: string) => action(storeId, transferId, 'reject', { reason }),
  ship: (storeId: number, transferId: number) => action(storeId, transferId, 'ship'),
  receive: (storeId: number, transferId: number) => action(storeId, transferId, 'receive'),
  cancel: (storeId: number, transferId: number) => action(storeId, transferId, 'cancel'),
}
