import { useCallback, useState } from 'react'
import { useStockActions } from '../../features/stock/stockActionsContext'
import { transferApi } from '../../shared/api/transfer/transferService'
import type { Transfer, TransferAction } from '../../shared/api/transfer/transferService'
import { useResource } from '../../shared/hooks/useResource'
import { useCurrentStore } from '../../shared/store/storeContext'
import { Badge } from '../../shared/ui/Badge'
import { transferStatusTone } from '../../shared/ui/badgeTone'
import { useConfirm } from '../../shared/ui/confirmContext'
import { EmptyState, LoadState } from '../../shared/ui/EmptyState'
import { useToast } from '../../shared/ui/toastContext'
import { formatDateTimeKst, formatNumber } from '../../shared/utils/format'
import './transfers.css'

type Direction = 'INCOMING' | 'OUTGOING'

const ACTION_TEXT: Record<TransferAction, { label: string; done: string; tone: 'primary' | 'secondary' | 'danger' }> = {
  APPROVE: { label: '승인', done: '승인했습니다. 출고를 진행해 주세요.', tone: 'primary' },
  REJECT: { label: '거절', done: '거절했습니다.', tone: 'danger' },
  SHIP: { label: '출고', done: '출고했습니다. 우리 매장 재고가 빠졌습니다.', tone: 'primary' },
  RECEIVE: { label: '입고 확인', done: '입고했습니다. 우리 매장 재고에 더했습니다.', tone: 'primary' },
  CANCEL: { label: '요청 취소', done: '요청을 취소했습니다.', tone: 'secondary' },
}

/** SCR-08 매장 간 이동 (FR-IN-003). 요청 → 승인 → 출고 → 입고. 버튼은 서버가 준 availableActions 만 */
export default function TransfersPage() {
  const { store } = useCurrentStore()
  const { openTransferRequest, transferVersion } = useStockActions()
  const [direction, setDirection] = useState<Direction>('INCOMING')
  const loader = useCallback(
    () => transferApi.list(store.storeId),
    // 새 요청을 만들면 다시 읽는다
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [store.storeId, transferVersion],
  )
  const { data, loading, error, reload, replace } = useResource(loader)

  const incoming = (data ?? []).filter((transfer) => transfer.direction === 'INCOMING')
  const outgoing = (data ?? []).filter((transfer) => transfer.direction === 'OUTGOING')
  const rows = direction === 'INCOMING' ? incoming : outgoing
  const pending = (list: Transfer[]) => list.filter((transfer) => transfer.availableActions.length > 0).length

  return (
    <div className="page-content">
      <header className="page-header">
        <div>
          <h1>매장 간 이동</h1>
          <p className="muted">받는 매장이 요청하고, 보내는 매장이 승인·출고, 받는 매장이 입고를 확인합니다.</p>
        </div>
        <div className="header-actions">
          <button type="button" className="button secondary small" onClick={reload} disabled={loading}>
            새로고침
          </button>
          {store.canManageInventory && (
            <button type="button" className="button small" onClick={() => openTransferRequest()}>
              새 이동 요청
            </button>
          )}
        </div>
      </header>

      <div className="tabs" role="tablist" aria-label="이동 방향">
        <button type="button" role="tab" aria-selected={direction === 'INCOMING'} className={direction === 'INCOMING' ? 'selected' : ''} onClick={() => setDirection('INCOMING')}>
          받는 이동 {pending(incoming) > 0 && <span className="tab-count">{pending(incoming)}</span>}
        </button>
        <button type="button" role="tab" aria-selected={direction === 'OUTGOING'} className={direction === 'OUTGOING' ? 'selected' : ''} onClick={() => setDirection('OUTGOING')}>
          보내는 이동 {pending(outgoing) > 0 && <span className="tab-count">{pending(outgoing)}</span>}
        </button>
      </div>

      {!data && <LoadState loading={loading} error={error} onRetry={reload} />}
      {data && rows.length === 0 && (
        <EmptyState title={direction === 'INCOMING' ? '받을 이동이 없습니다.' : '보낼 이동이 없습니다.'}>
          {direction === 'INCOMING' ? '재고가 모자란 품목은 다른 매장에 이동을 요청할 수 있습니다.' : '다른 매장이 요청하면 여기에 나타납니다.'}
        </EmptyState>
      )}
      <ul className="transfer-list">
        {rows.map((transfer) => (
          <TransferCard
            key={transfer.transferId}
            transfer={transfer}
            onChanged={(updated) => replace((data ?? []).map((item) => (item.transferId === updated.transferId ? updated : item)))}
          />
        ))}
      </ul>
    </div>
  )
}

function TransferCard({ transfer, onChanged }: { transfer: Transfer; onChanged: (transfer: Transfer) => void }) {
  const { store } = useCurrentStore()
  const confirm = useConfirm()
  const toast = useToast()
  const [busy, setBusy] = useState(false)
  const counterpart = transfer.direction === 'INCOMING' ? transfer.fromStoreName : transfer.toStoreName

  const act = async (action: TransferAction) => {
    let reason = ''
    if (action === 'REJECT') {
      const answer = await confirm({
        title: '이동 요청을 거절할까요?',
        confirmLabel: '거절',
        tone: 'danger',
        input: { label: '거절 사유', placeholder: '예: 이 매장도 재고가 부족합니다', required: true },
      })
      if (typeof answer !== 'string') return
      reason = answer
    } else if (action === 'SHIP' || action === 'CANCEL') {
      const ok = await confirm(
        action === 'SHIP'
          ? { title: '출고할까요?', message: `${transfer.toStoreName}(으)로 보내고 우리 매장 재고에서 뺍니다.`, confirmLabel: '출고' }
          : { title: '이동 요청을 취소할까요?', confirmLabel: '요청 취소', tone: 'danger' },
      )
      if (!ok) return
    }
    setBusy(true)
    try {
      const id = transfer.transferId
      const updated =
        action === 'APPROVE'
          ? await transferApi.approve(store.storeId, id)
          : action === 'REJECT'
            ? await transferApi.reject(store.storeId, id, reason)
            : action === 'SHIP'
              ? await transferApi.ship(store.storeId, id)
              : action === 'RECEIVE'
                ? await transferApi.receive(store.storeId, id)
                : await transferApi.cancel(store.storeId, id)
      toast.show(ACTION_TEXT[action].done)
      onChanged(updated)
    } catch (caught) {
      toast.error(caught, `${ACTION_TEXT[action].label}하지 못했습니다.`)
    } finally {
      setBusy(false)
    }
  }

  return (
    <li className="transfer-card">
      <header className="transfer-head">
        <div>
          <span className="muted small">#{transfer.transferId}</span>{' '}
          <strong>
            {transfer.fromStoreName} → {transfer.toStoreName}
          </strong>
          <span className="muted small"> · {transfer.direction === 'INCOMING' ? `${counterpart}에서 받음` : `${counterpart}(으)로 보냄`}</span>
        </div>
        <Badge tone={transferStatusTone(transfer.status.code)}>{transfer.status.description}</Badge>
      </header>

      <ul className="transfer-lines">
        {transfer.lines.map((line) => (
          <li key={line.skuId}>
            <span>{line.itemName}</span>
            <strong>{formatNumber(line.quantity)}</strong>
          </li>
        ))}
      </ul>

      <TransferSteps transfer={transfer} />

      {transfer.memo && <p className="small">메모: {transfer.memo}</p>}
      {transfer.rejectReason && <p className="small danger-text">거절 사유: {transfer.rejectReason}</p>}

      {transfer.availableActions.length > 0 && (
        <div className="transfer-actions">
          {transfer.availableActions.map((action) => (
            <button
              key={action}
              type="button"
              className={`button small ${ACTION_TEXT[action].tone === 'primary' ? '' : ACTION_TEXT[action].tone}`}
              disabled={busy}
              onClick={() => void act(action)}
            >
              {ACTION_TEXT[action].label}
            </button>
          ))}
        </div>
      )}
    </li>
  )
}

const STEPS: { key: keyof Transfer; label: string }[] = [
  { key: 'requestedAt', label: '요청' },
  { key: 'approvedAt', label: '승인' },
  { key: 'shippedAt', label: '출고' },
  { key: 'receivedAt', label: '입고' },
]

/** 4단계 진행. 거절·취소되면 멈춘 단계 뒤에 종료 표시 */
function TransferSteps({ transfer }: { transfer: Transfer }) {
  const closed = transfer.status.code === 'REJECTED' || transfer.status.code === 'CANCELLED'
  const doneCount = STEPS.filter((step) => transfer[step.key]).length

  return (
    <ol className={`steps ${closed ? 'is-closed' : ''}`} aria-label={`진행 상태: ${transfer.status.description}`}>
      {STEPS.map((step, index) => {
        const at = transfer[step.key] as string | null
        const state = at ? 'done' : !closed && index === doneCount ? 'current' : 'todo'
        return (
          <li key={step.label} className={`step step-${state}`}>
            <span className="step-dot" aria-hidden="true" />
            <span className="step-label">{step.label}</span>
            <span className="step-time">{at ? formatDateTimeKst(at) : ''}</span>
          </li>
        )
      })}
      {closed && (
        <li className="step step-closed">
          <span className="step-dot" aria-hidden="true" />
          <span className="step-label">{transfer.status.description}</span>
          <span className="step-time">{formatDateTimeKst(transfer.closedAt)}</span>
        </li>
      )}
    </ol>
  )
}
