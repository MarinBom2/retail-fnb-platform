import { useCallback, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { useStockActions } from '../../features/stock/stockActionsContext'
import type { CatalogProduct } from '../../shared/api/catalog/catalogService'
import { orderApi } from '../../shared/api/order/orderService'
import type { Order, PaymentMethodCode } from '../../shared/api/order/orderService'
import { useResource } from '../../shared/hooks/useResource'
import { useCurrentStore } from '../../shared/store/storeContext'
import { useConfirm } from '../../shared/ui/confirmContext'
import { EmptyState } from '../../shared/ui/EmptyState'
import { useToast } from '../../shared/ui/toastContext'
import { formatTimeKst, formatWon } from '../../shared/utils/format'
import { PaymentDialog } from './PaymentDialog'
import { findSku, outOfStockSkuId } from './posUtils'
import { QuantityStepper } from './RetailPos'

type Props = { catalog: CatalogProduct[]; onCatalogChanged: () => void }

const orderTitle = (order: Order) =>
  order.orderType.code === 'DINE_IN' ? `테이블 ${order.tableLabel}` : `포장 #${order.orderId}`

/** 외식 POS. 테이블(매장 식사)·포장 주문을 열어 두고 메뉴를 더하다가 마지막에 결제한다 */
export function FoodServicePos({ catalog, onCatalogChanged }: Props) {
  const { store } = useCurrentStore()
  const toast = useToast()
  const confirm = useConfirm()
  const { openAvailability } = useStockActions()

  const openLoader = useCallback(() => orderApi.list(store.storeId, 'OPEN'), [store.storeId])
  const openOrders = useResource(openLoader)

  const [currentId, setCurrentId] = useState<number | null>(null)
  const [current, setCurrent] = useState<Order | null>(null)
  const [tableLabel, setTableLabel] = useState('')
  const [category, setCategory] = useState<string>('전체')
  const [busy, setBusy] = useState(false)
  const [payOpen, setPayOpen] = useState(false)

  // 목록을 다시 읽어도 고른 주문은 최신 값으로 보여 준다
  const selected = (currentId && openOrders.data?.find((order) => order.orderId === currentId)) || (current?.orderId === currentId ? current : null)

  const menus = useMemo(
    () => catalog.filter((product) => product.kind.code === 'MENU' && product.skus.some((sku) => sku.sellable)),
    [catalog],
  )
  const categories = ['전체', ...new Set(menus.map((product) => product.category ?? '기타'))]
  const visibleMenus = category === '전체' ? menus : menus.filter((product) => (product.category ?? '기타') === category)

  const select = (order: Order | null) => {
    setCurrentId(order?.orderId ?? null)
    setCurrent(order)
  }

  const run = async (work: () => Promise<Order | null>, fallback: string) => {
    setBusy(true)
    try {
      const updated = await work()
      if (updated) select(updated.status.code === 'OPEN' ? updated : null)
      openOrders.reload()
      return updated
    } catch (caught) {
      toast.error(caught, fallback)
      return null
    } finally {
      setBusy(false)
    }
  }

  const openTable = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const label = tableLabel.trim()
    if (!label) return
    const inUse = openOrders.data?.find((order) => order.orderType.code === 'DINE_IN' && order.tableLabel === label)
    if (inUse) {
      select(inUse)
      setTableLabel('')
      return
    }
    void run(() => orderApi.create(store.storeId, { orderType: 'DINE_IN', tableLabel: label, items: [] }), '테이블을 열지 못했습니다.').then(
      (order) => order && setTableLabel(''),
    )
  }

  const openTakeout = () =>
    void run(() => orderApi.create(store.storeId, { orderType: 'TAKEOUT', items: [] }), '포장 주문을 만들지 못했습니다.')

  const addMenu = (skuId: number) => {
    if (!selected) {
      toast.show('테이블을 열거나 포장 주문을 먼저 만드세요.', 'info')
      return
    }
    void run(() => orderApi.addItem(store.storeId, selected.orderId, skuId, 1), '메뉴를 더하지 못했습니다.')
  }

  const changeQuantity = (skuId: number, quantity: number) => {
    if (!selected) return
    void run(
      () =>
        quantity <= 0
          ? orderApi.removeItem(store.storeId, selected.orderId, skuId)
          : orderApi.changeQuantity(store.storeId, selected.orderId, skuId, quantity),
      '수량을 바꾸지 못했습니다.',
    )
  }

  const cancelOrder = async () => {
    if (!selected) return
    const ok = await confirm({ title: `${orderTitle(selected)} 주문을 취소할까요?`, message: '결제 전 주문이라 재고는 바뀌지 않습니다.', confirmLabel: '주문 취소', tone: 'danger' })
    if (!ok) return
    const cancelled = await run(() => orderApi.cancel(store.storeId, selected.orderId), '주문을 취소하지 못했습니다.')
    if (cancelled) toast.show('주문을 취소했습니다.')
  }

  const pay = async (method: PaymentMethodCode) => {
    if (!selected) return
    setBusy(true)
    try {
      const paid = await orderApi.pay(store.storeId, selected.orderId, method, selected.totalAmount)
      toast.show(`${orderTitle(paid)} ${formatWon(paid.totalAmount)} 결제했습니다.`)
      setPayOpen(false)
      select(null)
      openOrders.reload()
      onCatalogChanged()
    } catch (caught) {
      const skuId = outOfStockSkuId(caught)
      if (skuId) {
        // 테이블 주문은 그대로 열어 둔다. 모자란 것은 식자재다
        setPayOpen(false)
        const ingredient = findSku(catalog, skuId)
        openAvailability({
          skuId,
          itemName: ingredient?.displayName,
          reason: `식자재 ${ingredient?.displayName ?? ''}이(가) 모자라 결제할 수 없습니다. 주문은 열려 있습니다.`,
        })
      } else {
        toast.error(caught, '결제하지 못했습니다.')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="pos pos-food">
      <section className="pos-tables" aria-label="이용 중인 주문">
        <form className="table-open" onSubmit={openTable}>
          <label className="visually-hidden" htmlFor="table-label">
            테이블 번호
          </label>
          <input
            id="table-label"
            value={tableLabel}
            maxLength={20}
            inputMode="numeric"
            placeholder="번호"
            onChange={(event) => setTableLabel(event.target.value)}
          />
          <button type="submit" className="button small" disabled={busy || !tableLabel.trim()}>
            열기
          </button>
        </form>
        <button type="button" className="button secondary small block" disabled={busy} onClick={openTakeout}>
          + 포장 주문
        </button>
        <h2 className="section-label">이용 중 {openOrders.data ? `(${openOrders.data.length})` : ''}</h2>
        {openOrders.data?.length === 0 && <p className="muted small">열린 주문이 없습니다.</p>}
        <ul className="table-list">
          {openOrders.data?.map((order) => (
            <li key={order.orderId}>
              <button
                type="button"
                className={`table-chip ${selected?.orderId === order.orderId ? 'selected' : ''}`}
                aria-pressed={selected?.orderId === order.orderId}
                onClick={() => select(order)}
              >
                <strong>{orderTitle(order)}</strong>
                <span className="small">{formatWon(order.totalAmount)}</span>
                <span className="muted small">{formatTimeKst(order.createdAt)}</span>
              </button>
            </li>
          ))}
        </ul>
      </section>

      <section className="pos-catalog" aria-label="메뉴">
        <div className="chip-row" role="tablist" aria-label="메뉴 분류">
          {categories.map((name) => (
            <button
              key={name}
              type="button"
              role="tab"
              aria-selected={category === name}
              className={`chip ${category === name ? 'selected' : ''}`}
              onClick={() => setCategory(name)}
            >
              {name}
            </button>
          ))}
        </div>
        {visibleMenus.length === 0 ? (
          <EmptyState title="판매 중인 메뉴가 없습니다." />
        ) : (
          <div className="product-grid">
            {visibleMenus.map((product) => {
              const sku = product.skus.find((candidate) => candidate.sellable)
              if (!sku) return null
              return (
                <button key={sku.skuId} type="button" className="product-tile" disabled={busy} onClick={() => addMenu(sku.skuId)}>
                  <span className="tile-name">{product.name}</span>
                  <span className="tile-price">{formatWon(sku.price)}</span>
                </button>
              )
            })}
          </div>
        )}
      </section>

      <aside className="pos-cart" aria-label="선택한 주문">
        <div className="cart-header">
          <h2>{selected ? orderTitle(selected) : '주문을 고르세요'}</h2>
          {selected && (
            <button type="button" className="link small danger-text" onClick={() => void cancelOrder()} disabled={busy}>
              주문 취소
            </button>
          )}
        </div>
        {!selected ? (
          <EmptyState title="테이블을 열거나 이용 중인 주문을 고르세요." />
        ) : selected.lines.length === 0 ? (
          <EmptyState title="아직 담긴 메뉴가 없습니다.">메뉴를 누르면 이 주문에 더해집니다.</EmptyState>
        ) : (
          <ul className="cart-lines">
            {selected.lines.map((line) => (
              <li key={line.skuId}>
                <div className="grow">
                  <span className="line-name">{line.itemName}</span>
                  <span className="muted small">{formatWon(line.unitPrice)}</span>
                </div>
                <QuantityStepper
                  value={line.quantity}
                  label={line.itemName}
                  disabled={busy}
                  onChange={(quantity) => changeQuantity(line.skuId, quantity)}
                />
                <strong className="line-amount">{formatWon(line.lineAmount)}</strong>
              </li>
            ))}
          </ul>
        )}
        <div className="cart-footer">
          <div className="cart-total">
            <span>합계</span>
            <strong>{formatWon(selected?.totalAmount ?? 0)}</strong>
          </div>
          <button
            type="button"
            className="button large block"
            disabled={!selected || selected.lines.length === 0 || busy}
            onClick={() => setPayOpen(true)}
          >
            결제하기
          </button>
          <p className="muted small">결제할 때 레시피대로 식자재 재고가 빠집니다.</p>
        </div>
      </aside>

      <div className="cart-bar">
        <div className="cart-bar-total">
          <span className="muted small">{selected ? orderTitle(selected) : '주문 없음'}</span>
          <strong>{formatWon(selected?.totalAmount ?? 0)}</strong>
        </div>
        <button type="button" className="button" disabled={!selected || selected.lines.length === 0 || busy} onClick={() => setPayOpen(true)}>
          결제하기
        </button>
      </div>

      {payOpen && selected && (
        <PaymentDialog
          title={`${orderTitle(selected)} 결제`}
          total={selected.totalAmount}
          busy={busy}
          onClose={() => setPayOpen(false)}
          onPay={(method) => void pay(method)}
        />
      )}
    </div>
  )
}
