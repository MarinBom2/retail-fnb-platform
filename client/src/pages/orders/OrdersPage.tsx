import { useCallback, useState } from 'react'
import { orderApi } from '../../shared/api/order/orderService'
import type { Order, OrderStatusCode } from '../../shared/api/order/orderService'
import { useResource } from '../../shared/hooks/useResource'
import { useCurrentStore } from '../../shared/store/storeContext'
import { Badge } from '../../shared/ui/Badge'
import { orderStatusTone } from '../../shared/ui/badgeTone'
import { useConfirm } from '../../shared/ui/confirmContext'
import { EmptyState, LoadState } from '../../shared/ui/EmptyState'
import { useToast } from '../../shared/ui/toastContext'
import { formatDateTimeKst, formatNumber, formatWon } from '../../shared/utils/format'
import './orders.css'

const TABS: { code: OrderStatusCode | undefined; label: string }[] = [
  { code: undefined, label: '전체' },
  { code: 'OPEN', label: '주문 중' },
  { code: 'PAID', label: '결제 완료' },
  { code: 'CANCELLED', label: '취소' },
  { code: 'REFUNDED', label: '환불' },
]

/** SCR-06 주문 내역 (FR-OR-005). 결제 전 취소(FR-OR-003), 전액 환불(FR-OR-004) */
export default function OrdersPage() {
  const { store } = useCurrentStore()
  const [status, setStatus] = useState<OrderStatusCode | undefined>(undefined)
  const loader = useCallback(() => orderApi.list(store.storeId, status), [store.storeId, status])
  const { data, loading, error, reload } = useResource(loader)

  return (
    <div className="page-content">
      <header className="page-header">
        <div>
          <h1>주문 내역</h1>
          <p className="muted">최근 50건</p>
        </div>
        <button type="button" className="button secondary small" onClick={reload} disabled={loading}>
          새로고침
        </button>
      </header>

      <div className="tabs" role="tablist" aria-label="주문 상태">
        {TABS.map((tab) => (
          <button
            key={tab.label}
            type="button"
            role="tab"
            aria-selected={status === tab.code}
            className={status === tab.code ? 'selected' : ''}
            onClick={() => setStatus(tab.code)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {!data && <LoadState loading={loading} error={error} onRetry={reload} />}
      {data && data.length === 0 && <EmptyState title="주문이 없습니다." />}
      {data && data.length > 0 && (
        <ul className="order-list">
          {data.map((order) => (
            <OrderRow key={order.orderId} order={order} onChanged={reload} />
          ))}
        </ul>
      )}
    </div>
  )
}

function OrderRow({ order, onChanged }: { order: Order; onChanged: () => void }) {
  const { store } = useCurrentStore()
  const confirm = useConfirm()
  const toast = useToast()
  const [busy, setBusy] = useState(false)
  const itemCount = order.lines.reduce((sum, line) => sum + line.quantity, 0)
  const title = order.tableLabel ? `테이블 ${order.tableLabel}` : order.orderType.description

  const act = async (kind: 'cancel' | 'refund') => {
    const ok = await confirm(
      kind === 'refund'
        ? {
            title: `주문 #${order.orderId}을 환불할까요?`,
            message: `${formatWon(order.totalAmount)} 전액 환불하고, 결제 때 빠진 재고를 되돌립니다.`,
            confirmLabel: '환불',
            tone: 'danger',
          }
        : { title: `주문 #${order.orderId}을 취소할까요?`, message: '결제 전 주문이라 재고는 바뀌지 않습니다.', confirmLabel: '주문 취소', tone: 'danger' },
    )
    if (!ok) return
    setBusy(true)
    try {
      await (kind === 'refund' ? orderApi.refund(store.storeId, order.orderId) : orderApi.cancel(store.storeId, order.orderId))
      toast.show(kind === 'refund' ? '환불했습니다. 재고를 되돌렸습니다.' : '주문을 취소했습니다.')
      onChanged()
    } catch (caught) {
      toast.error(caught, kind === 'refund' ? '환불하지 못했습니다.' : '취소하지 못했습니다.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <li className="order-row">
      <details>
        <summary>
          <span className="order-id">#{order.orderId}</span>
          <span className="order-title">
            <strong>{title}</strong>
            <span className="muted small">
              {formatDateTimeKst(order.paidAt ?? order.createdAt)} · {formatNumber(itemCount)}개
              {order.paymentMethod ? ` · ${order.paymentMethod.description}` : ''}
            </span>
          </span>
          <Badge tone={orderStatusTone(order.status.code)}>{order.status.description}</Badge>
          <strong className="order-amount">{formatWon(order.totalAmount)}</strong>
        </summary>
        <div className="order-detail">
          <table className="data-table compact">
            <thead>
              <tr>
                <th scope="col">상품</th>
                <th scope="col" className="num">단가</th>
                <th scope="col" className="num">수량</th>
                <th scope="col" className="num">금액</th>
              </tr>
            </thead>
            <tbody>
              {order.lines.map((line) => (
                <tr key={line.skuId}>
                  <td data-label="상품">{line.itemName}</td>
                  <td data-label="단가" className="num">{formatWon(line.unitPrice)}</td>
                  <td data-label="수량" className="num">{formatNumber(line.quantity)}</td>
                  <td data-label="금액" className="num">{formatWon(line.lineAmount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="order-actions">
            {order.status.code === 'OPEN' && (
              <button type="button" className="button secondary small" disabled={busy} onClick={() => void act('cancel')}>
                결제 전 취소
              </button>
            )}
            {order.status.code === 'PAID' && (
              <button type="button" className="button danger small" disabled={busy} onClick={() => void act('refund')}>
                환불
              </button>
            )}
          </div>
        </div>
      </details>
    </li>
  )
}
