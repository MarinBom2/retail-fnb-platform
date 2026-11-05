import { useCallback } from 'react'
import { inventoryApi } from '../../shared/api/inventory/inventoryService'
import { useResource } from '../../shared/hooks/useResource'
import { useCurrentStore } from '../../shared/store/storeContext'
import { Dialog } from '../../shared/ui/Dialog'
import { EmptyState, LoadState } from '../../shared/ui/EmptyState'
import { formatNumber } from '../../shared/utils/format'
import type { AvailabilityRequest, TransferPrefill } from './stockActionsContext'

type Props = {
  request: AvailabilityRequest
  onClose: () => void
  onRequestTransfer: (prefill: TransferPrefill) => void
}

/** FR-IN-006 같은 브랜드 다른 매장 재고. 판매 기회를 놓치지 않게 바로 이동 요청으로 잇는다 */
export function AvailabilityDialog({ request, onClose, onRequestTransfer }: Props) {
  const { store } = useCurrentStore()
  const loader = useCallback(() => inventoryApi.availability(store.storeId, request.skuId), [store.storeId, request.skuId])
  const { data, loading, error, reload } = useResource(loader)

  return (
    <Dialog
      open
      title={request.reason ? '재고가 부족합니다' : '다른 매장 재고'}
      description={request.itemName}
      onClose={onClose}
      size="sm"
    >
      {request.reason && <p className="notice-box">{request.reason}</p>}
      <LoadState loading={loading && !data} error={error} onRetry={reload} />
      {data && data.length === 0 && (
        <EmptyState title="재고가 있는 다른 매장이 없습니다.">입고 일정을 확인하거나 본사에 문의해 주세요.</EmptyState>
      )}
      {data && data.length > 0 && (
        <ul className="availability-list">
          {data.map((row) => (
            <li key={row.storeId}>
              <div>
                <strong>{row.storeName}</strong>
                <span className="muted small"> 재고 {formatNumber(row.quantity)}</span>
              </div>
              {store.canManageInventory ? (
                <button
                  type="button"
                  className="button small"
                  onClick={() => onRequestTransfer({ skuId: request.skuId, fromStoreId: row.storeId, quantity: 1 })}
                >
                  이 매장에서 이동 요청
                </button>
              ) : null}
            </li>
          ))}
        </ul>
      )}
      {!store.canManageInventory && data && data.length > 0 && (
        <p className="muted small">이동 요청은 점주·매니저가 할 수 있습니다.</p>
      )}
    </Dialog>
  )
}
