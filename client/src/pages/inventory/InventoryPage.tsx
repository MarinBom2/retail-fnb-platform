import { useCallback, useState } from 'react'
import type { FormEvent } from 'react'
import { useStockActions } from '../../features/stock/stockActionsContext'
import { inventoryApi } from '../../shared/api/inventory/inventoryService'
import type { StockItem } from '../../shared/api/inventory/inventoryService'
import { useResource } from '../../shared/hooks/useResource'
import { isFoodService, useCurrentStore } from '../../shared/store/storeContext'
import { Badge } from '../../shared/ui/Badge'
import { Dialog } from '../../shared/ui/Dialog'
import { EmptyState, LoadState } from '../../shared/ui/EmptyState'
import { IconRefresh, IconSearch } from '../../shared/ui/icons'
import { Menu } from '../../shared/ui/Menu'
import { Segmented } from '../../shared/ui/Segmented'
import { useToast } from '../../shared/ui/toastContext'
import { formatDateTimeKst, formatNumber, formatQuantity } from '../../shared/utils/format'
import './inventory.css'

type EditKind = 'receive' | 'adjust' | 'reorder'

/** SCR-07 매장 재고 (FR-IN-001·002·004·005·006). 입고·실사·안전재고는 점주·매니저만 */
export default function InventoryPage() {
  const { store } = useCurrentStore()
  const { openAvailability } = useStockActions()
  const loader = useCallback(() => inventoryApi.list(store.storeId), [store.storeId])
  const { data, loading, error, reload, replace } = useResource(loader)

  const [query, setQuery] = useState('')
  const [onlyLow, setOnlyLow] = useState(false)
  const [edit, setEdit] = useState<{ kind: EditKind; item: StockItem } | null>(null)
  const [history, setHistory] = useState<StockItem | null>(null)

  const keyword = query.trim().toLowerCase()
  const rows = (data ?? []).filter(
    (item) =>
      (!onlyLow || item.needsReorder) &&
      (!keyword || item.itemName.toLowerCase().includes(keyword) || item.skuCode.toLowerCase().includes(keyword)),
  )
  const lowCount = (data ?? []).filter((item) => item.needsReorder).length

  const updateRow = (updated: StockItem) => replace((data ?? []).map((item) => (item.skuId === updated.skuId ? updated : item)))

  return (
    <div className="page-content">
      <header className="page-header">
        <div>
          <h1>재고</h1>
          <p className="muted">
            {isFoodService(store) ? '식자재' : '판매 상품'} {formatNumber(data?.length ?? 0)}품목 · 안전재고 이하 {formatNumber(lowCount)}
          </p>
        </div>
        <button type="button" className="button secondary small" onClick={reload} disabled={loading}>
          <IconRefresh className="button-icon" />
          새로고침
        </button>
      </header>

      {!store.canManageInventory && (
        <p className="notice-box">재고 수량을 바꾸는 일(입고·실사·안전재고·이동 요청)은 점주·매니저가 할 수 있습니다. 지금은 보기만 됩니다.</p>
      )}

      <div className="toolbar">
        <div className="search-field">
          <IconSearch />
          <label className="visually-hidden" htmlFor="stock-search">
            재고 검색
          </label>
          <input
            id="stock-search"
            type="search"
            value={query}
            placeholder="상품 이름·SKU 코드"
            onChange={(event) => setQuery(event.target.value)}
          />
        </div>
        <Segmented
          label="재고 상태"
          value={onlyLow ? 'low' : 'all'}
          onChange={(value) => setOnlyLow(value === 'low')}
          options={[
            { value: 'all', label: '전체' },
            {
              value: 'low',
              label: (
                <>
                  안전재고 이하{lowCount > 0 && <span className="seg-count">{lowCount}</span>}
                </>
              ),
            },
          ]}
        />
      </div>

      {!data && <LoadState loading={loading} error={error} onRetry={reload} />}
      {data && rows.length === 0 && <EmptyState title={onlyLow ? '안전재고 이하 품목이 없습니다.' : '품목이 없습니다.'} />}
      {rows.length > 0 && (
        <div className="table-wrap">
          <table className="data-table responsive inventory-table">
            <thead>
              <tr>
                <th scope="col">품목</th>
                <th scope="col" className="num">현재 재고</th>
                <th scope="col" className="num">안전재고</th>
                <th scope="col">상태</th>
                <th scope="col" className="actions-col">
                  <span className="visually-hidden">작업</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {rows.map((item) => (
                <tr key={item.skuId} className={item.needsReorder ? 'row-warning' : ''}>
                  <td data-label="품목">
                    <span className="stock-name">
                      <strong>{item.itemName}</strong>
                      <span className="small">{item.skuCode}</span>
                    </span>
                  </td>
                  <td data-label="현재 재고" className="num">
                    {formatQuantity(item.quantity, item.unit)}
                  </td>
                  <td data-label="안전재고" className="num">
                    {item.reorderPoint > 0 ? formatQuantity(item.reorderPoint, item.unit) : <span className="muted">미설정</span>}
                  </td>
                  <td data-label="상태">
                    {item.quantity === 0 ? (
                      <Badge tone="danger">품절</Badge>
                    ) : item.needsReorder ? (
                      <Badge tone="warning">보충 필요</Badge>
                    ) : (
                      <Badge tone="success">정상</Badge>
                    )}
                  </td>
                  <td className="row-actions">
                    {store.canManageInventory && (
                      <button type="button" className="button small" onClick={() => setEdit({ kind: 'receive', item })}>
                        입고
                      </button>
                    )}
                    <Menu
                      label={`${item.itemName} 더 보기`}
                      items={[
                        ...(store.canManageInventory
                          ? [
                              { label: '실사', onSelect: () => setEdit({ kind: 'adjust', item }) },
                              { label: '안전재고 설정', onSelect: () => setEdit({ kind: 'reorder', item }) },
                            ]
                          : []),
                        { label: '다른 매장 재고', onSelect: () => openAvailability({ skuId: item.skuId, itemName: item.itemName }) },
                        { label: '변동 이력', onSelect: () => setHistory(item) },
                      ]}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {edit && (
        <StockEditDialog
          key={`${edit.kind}-${edit.item.skuId}`}
          kind={edit.kind}
          item={edit.item}
          onClose={() => setEdit(null)}
          onSaved={(updated) => {
            updateRow(updated)
            setEdit(null)
          }}
        />
      )}
      {history && <MovementsDialog key={history.skuId} item={history} onClose={() => setHistory(null)} />}
    </div>
  )
}

const EDIT_TEXT: Record<EditKind, { title: string; label: string; done: string; min: number }> = {
  receive: { title: '입고', label: '입고 수량', done: '입고했습니다.', min: 1 },
  adjust: { title: '실사', label: '센 수량', done: '실사 수량으로 맞췄습니다.', min: 0 },
  reorder: { title: '안전재고 설정', label: '안전재고 (이 수량 이하이면 알림)', done: '안전재고를 바꿨습니다.', min: 0 },
}

function StockEditDialog({
  kind,
  item,
  onClose,
  onSaved,
}: {
  kind: EditKind
  item: StockItem
  onClose: () => void
  onSaved: (item: StockItem) => void
}) {
  const { store } = useCurrentStore()
  const toast = useToast()
  const text = EDIT_TEXT[kind]
  const initial = kind === 'receive' ? '' : String(kind === 'adjust' ? item.quantity : item.reorderPoint)
  const [value, setValue] = useState(initial)
  const [busy, setBusy] = useState(false)
  const amount = Number(value)
  const valid = value !== '' && Number.isInteger(amount) && amount >= text.min

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!valid) return
    setBusy(true)
    try {
      const updated =
        kind === 'receive'
          ? await inventoryApi.receive(store.storeId, item.skuId, amount)
          : kind === 'adjust'
            ? await inventoryApi.adjust(store.storeId, item.skuId, amount)
            : await inventoryApi.changeReorderPoint(store.storeId, item.skuId, amount)
      toast.show(text.done)
      onSaved(updated)
    } catch (caught) {
      toast.error(caught, `${text.title}하지 못했습니다.`)
    } finally {
      setBusy(false)
    }
  }

  const preview =
    valid && kind === 'receive'
      ? `입고 후 ${formatQuantity(item.quantity + amount, item.unit)}`
      : valid && kind === 'adjust'
        ? `차이 ${amount - item.quantity >= 0 ? '+' : ''}${formatNumber(amount - item.quantity)}${item.unit.description}`
        : null

  return (
    <Dialog open title={text.title} description={`${item.itemName} · 현재 ${formatQuantity(item.quantity, item.unit)}`} size="sm" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <label>
          {text.label} ({item.unit.description})
          <input
            type="number"
            inputMode="numeric"
            min={text.min}
            step={1}
            required
            value={value}
            onChange={(event) => setValue(event.target.value)}
          />
        </label>
        {preview && <p className="muted small">{preview}</p>}
        <div className="dialog-actions">
          <button type="button" className="button secondary" onClick={onClose}>
            닫기
          </button>
          <button type="submit" className="button" disabled={!valid || busy}>
            {busy ? '저장 중…' : '저장'}
          </button>
        </div>
      </form>
    </Dialog>
  )
}

function MovementsDialog({ item, onClose }: { item: StockItem; onClose: () => void }) {
  const { store } = useCurrentStore()
  const loader = useCallback(() => inventoryApi.movements(store.storeId, item.skuId), [store.storeId, item.skuId])
  const { data, loading, error, reload } = useResource(loader)

  return (
    <Dialog open title="재고 변동 이력" description={`${item.itemName} · 최근 50건`} size="lg" onClose={onClose}>
      {!data && <LoadState loading={loading} error={error} onRetry={reload} />}
      {data && data.length === 0 && <EmptyState title="변동 기록이 없습니다." />}
      {data && data.length > 0 && (
        <div className="table-wrap">
          <table className="data-table compact responsive">
            <thead>
              <tr>
                <th scope="col">시각</th>
                <th scope="col">구분</th>
                <th scope="col" className="num">변동</th>
                <th scope="col" className="num">변동 후</th>
                <th scope="col">출처</th>
              </tr>
            </thead>
            <tbody>
              {data.map((movement) => (
                <tr key={movement.movementId}>
                  <td data-label="시각">{formatDateTimeKst(movement.createdAt)}</td>
                  <td data-label="구분">{movement.type.description}</td>
                  <td data-label="변동" className={`num ${movement.delta < 0 ? 'negative' : movement.delta > 0 ? 'positive' : ''}`}>
                    {movement.delta > 0 ? '+' : ''}
                    {formatNumber(movement.delta)}
                  </td>
                  <td data-label="변동 후" className="num">
                    {formatQuantity(movement.balanceAfter, item.unit)}
                  </td>
                  <td data-label="출처" className="muted small">
                    {movement.referenceType.description}
                    {movement.referenceId ? ` #${movement.referenceId}` : ''}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Dialog>
  )
}
