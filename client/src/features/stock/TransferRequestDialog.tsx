import { useCallback, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { catalogApi } from '../../shared/api/catalog/catalogService'
import { inventoryApi } from '../../shared/api/inventory/inventoryService'
import type { StoreAvailability } from '../../shared/api/inventory/inventoryService'
import { transferApi } from '../../shared/api/transfer/transferService'
import { useResource } from '../../shared/hooks/useResource'
import { useCurrentStore } from '../../shared/store/storeContext'
import { Dialog } from '../../shared/ui/Dialog'
import { useToast } from '../../shared/ui/toastContext'
import { formatNumber } from '../../shared/utils/format'
import type { TransferPrefill } from './stockActionsContext'

type Props = {
  prefill: TransferPrefill
  onClose: () => void
  onCreated: () => void
}

/** FR-IN-003 이동 요청. 지금 매장이 받을 매장이고, 재고가 있는 매장을 골라 요청한다 */
export function TransferRequestDialog({ prefill, onClose, onCreated }: Props) {
  const { store } = useCurrentStore()
  const toast = useToast()

  const catalogLoader = useCallback(() => catalogApi.list(store.storeId), [store.storeId])
  const catalog = useResource(catalogLoader)
  const skus = useMemo(
    () =>
      (catalog.data ?? []).flatMap((product) =>
        product.skus.filter((sku) => sku.stockTracked).map((sku) => ({ ...sku, productName: product.name })),
      ),
    [catalog.data],
  )

  const [skuId, setSkuId] = useState<number | null>(prefill.skuId ?? null)
  const [fromStoreId, setFromStoreId] = useState<number | null>(prefill.fromStoreId ?? null)
  const [quantity, setQuantity] = useState(String(prefill.quantity ?? 1))
  const [memo, setMemo] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const selectedSku = skus.find((sku) => sku.skuId === skuId)

  return (
    <Dialog open title="매장 간 이동 요청" description={`받는 매장: ${store.storeName}`} onClose={onClose}>
      <form
        className="form"
        onSubmit={async (event: FormEvent<HTMLFormElement>) => {
          event.preventDefault()
          const amount = Number(quantity)
          if (!skuId || !fromStoreId || !Number.isInteger(amount) || amount < 1) return
          setSubmitting(true)
          try {
            await transferApi.request(store.storeId, {
              fromStoreId,
              lines: [{ skuId, quantity: amount }],
              memo: memo.trim() || undefined,
            })
            toast.show('이동을 요청했습니다. 보내는 매장이 승인하면 출고됩니다.')
            onCreated()
          } catch (caught) {
            toast.error(caught, '이동을 요청하지 못했습니다.')
          } finally {
            setSubmitting(false)
          }
        }}
      >
        <label>
          상품
          <select
            required
            value={skuId ?? ''}
            onChange={(event) => {
              setSkuId(event.target.value ? Number(event.target.value) : null)
              setFromStoreId(null)
            }}
          >
            <option value="">{catalog.loading && !catalog.data ? '불러오는 중…' : '상품을 고르세요'}</option>
            {skus.map((sku) => (
              <option key={sku.skuId} value={sku.skuId}>
                {sku.displayName} · 우리 매장 {formatNumber(sku.stockQuantity ?? 0)}
                {sku.unit.description}
              </option>
            ))}
          </select>
        </label>

        {skuId && (
          <SourceStorePicker
            key={skuId}
            storeId={store.storeId}
            skuId={skuId}
            value={fromStoreId}
            onChange={setFromStoreId}
          />
        )}

        <div className="form-row">
          <label>
            수량{selectedSku ? ` (${selectedSku.unit.description})` : ''}
            <input
              type="number"
              inputMode="numeric"
              min={1}
              step={1}
              required
              value={quantity}
              onChange={(event) => setQuantity(event.target.value)}
            />
          </label>
          <label className="grow">
            메모 (선택)
            <input value={memo} maxLength={200} placeholder="예: 주말 행사용" onChange={(event) => setMemo(event.target.value)} />
          </label>
        </div>

        <div className="dialog-actions">
          <button type="button" className="button secondary" onClick={onClose}>
            닫기
          </button>
          <button type="submit" className="button" disabled={submitting || !skuId || !fromStoreId}>
            {submitting ? '요청 중…' : '이동 요청'}
          </button>
        </div>
      </form>
    </Dialog>
  )
}

function SourceStorePicker({
  storeId,
  skuId,
  value,
  onChange,
}: {
  storeId: number
  skuId: number
  value: number | null
  onChange: (storeId: number) => void
}) {
  const loader = useCallback(() => inventoryApi.availability(storeId, skuId), [storeId, skuId])
  const { data, loading, error } = useResource<StoreAvailability[]>(loader)

  return (
    <fieldset className="choice-group">
      <legend>보낼 매장</legend>
      {loading && !data && <p className="muted small">재고가 있는 매장을 찾는 중…</p>}
      {error ? <p className="error small">매장 재고를 불러오지 못했습니다.</p> : null}
      {data && data.length === 0 && <p className="muted small">이 상품의 재고가 있는 다른 매장이 없습니다.</p>}
      {data?.map((row) => (
        <label key={row.storeId} className={`choice ${value === row.storeId ? 'selected' : ''}`}>
          <input
            type="radio"
            name="fromStore"
            value={row.storeId}
            checked={value === row.storeId}
            onChange={() => onChange(row.storeId)}
          />
          <span>{row.storeName}</span>
          <span className="muted small">재고 {formatNumber(row.quantity)}</span>
        </label>
      ))}
    </fieldset>
  )
}
