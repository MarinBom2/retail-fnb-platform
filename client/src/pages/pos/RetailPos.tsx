import { useMemo, useState } from 'react'
import type { KeyboardEvent } from 'react'
import { useStockActions } from '../../features/stock/stockActionsContext'
import type { CatalogProduct, CatalogSku } from '../../shared/api/catalog/catalogService'
import { orderApi } from '../../shared/api/order/orderService'
import type { Order, PaymentMethodCode } from '../../shared/api/order/orderService'
import { isApparel, useCurrentStore } from '../../shared/store/storeContext'
import { EmptyState } from '../../shared/ui/EmptyState'
import { useToast } from '../../shared/ui/toastContext'
import { formatNumber, formatWon } from '../../shared/utils/format'
import { ApparelMatrixDialog } from './ApparelMatrixDialog'
import { PaymentDialog } from './PaymentDialog'
import { findSku, outOfStockSkuId } from './posUtils'

type CartLine = { sku: CatalogSku; quantity: number }

type Props = { catalog: CatalogProduct[]; onCatalogChanged: () => void }

/** 계산대 POS (생활용품·의류). 장바구니는 화면에만 두고 결제할 때 주문을 만든다 */
export function RetailPos({ catalog, onCatalogChanged }: Props) {
  const { store } = useCurrentStore()
  const toast = useToast()
  const { openAvailability } = useStockActions()
  const apparel = isApparel(store)

  const [cart, setCart] = useState<CartLine[]>([])
  const [query, setQuery] = useState('')
  const [styleId, setStyleId] = useState<number | null>(null)
  const [paying, setPaying] = useState(false)
  const [payOpen, setPayOpen] = useState(false)
  const [lastPaid, setLastPaid] = useState<Order | null>(null)

  const products = useMemo(
    () => catalog.filter((product) => product.kind.code === 'MERCHANDISE' && product.skus.some((sku) => sku.sellable)),
    [catalog],
  )
  const skus = useMemo(() => products.flatMap((product) => product.skus.filter((sku) => sku.sellable)), [products])

  const keyword = query.trim().toLowerCase()
  const filteredProducts = keyword
    ? products.filter(
        (product) =>
          product.name.toLowerCase().includes(keyword) ||
          product.skus.some((sku) => sku.barcode === query.trim() || sku.skuCode.toLowerCase() === keyword),
      )
    : products

  const total = cart.reduce((sum, line) => sum + line.sku.price * line.quantity, 0)
  const itemCount = cart.reduce((sum, line) => sum + line.quantity, 0)

  const add = (sku: CatalogSku) => {
    setLastPaid(null)
    setCart((lines) => {
      const found = lines.find((line) => line.sku.skuId === sku.skuId)
      return found
        ? lines.map((line) => (line.sku.skuId === sku.skuId ? { ...line, quantity: line.quantity + 1 } : line))
        : [...lines, { sku, quantity: 1 }]
    })
  }

  const changeQuantity = (skuId: number, quantity: number) =>
    setCart((lines) =>
      quantity <= 0
        ? lines.filter((line) => line.sku.skuId !== skuId)
        : lines.map((line) => (line.sku.skuId === skuId ? { ...line, quantity } : line)),
    )

  /** 바코드·SKU 코드가 정확히 맞으면 바로 담는다. 검색 결과가 하나여도 담는다 */
  const handleScan = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key !== 'Enter') return
    event.preventDefault()
    const code = query.trim()
    if (!code) return
    const exact = skus.find((sku) => sku.barcode === code || sku.skuCode.toLowerCase() === code.toLowerCase())
    const single = !apparel && filteredProducts.length === 1 ? filteredProducts[0].skus.find((sku) => sku.sellable) : undefined
    const target = exact ?? single
    if (target) {
      add(target)
      setQuery('')
    } else if (apparel && filteredProducts.length === 1) {
      setStyleId(filteredProducts[0].productId)
    } else {
      toast.show(`"${code}" 와 맞는 상품이 없습니다.`, 'info')
    }
  }

  const checkout = async (method: PaymentMethodCode) => {
    setPaying(true)
    let created: Order | null = null
    try {
      created = await orderApi.create(store.storeId, {
        orderType: 'COUNTER',
        items: cart.map((line) => ({ skuId: line.sku.skuId, quantity: line.quantity })),
      })
      const paid = await orderApi.pay(store.storeId, created.orderId, method, created.totalAmount)
      toast.show(`${formatWon(paid.totalAmount)} 결제했습니다.`)
      setCart([])
      setPayOpen(false)
      setLastPaid(paid)
      onCatalogChanged()
    } catch (caught) {
      // 계산대 주문은 결제와 함께 끝나야 한다. 실패하면 만든 주문을 닫는다
      if (created) await orderApi.cancel(store.storeId, created.orderId).catch(() => undefined)
      const skuId = outOfStockSkuId(caught)
      if (skuId) {
        setPayOpen(false)
        const sku = findSku(catalog, skuId)
        openAvailability({ skuId, itemName: sku?.displayName, reason: `${sku?.displayName ?? '상품'}의 이 매장 재고가 모자랍니다.` })
      } else {
        toast.error(caught, '결제하지 못했습니다.')
      }
    } finally {
      setPaying(false)
    }
  }

  const style = styleId ? products.find((product) => product.productId === styleId) : undefined

  return (
    <div className="pos">
      <section className="pos-catalog" aria-label="상품">
        <div className="pos-search">
          <label className="visually-hidden" htmlFor="pos-search">
            {apparel ? '상품 검색' : '바코드 또는 상품 검색'}
          </label>
          <input
            id="pos-search"
            type="search"
            autoFocus
            value={query}
            placeholder={apparel ? '스타일 이름·SKU 코드' : '바코드를 찍거나 상품 이름을 입력하세요'}
            onChange={(event) => setQuery(event.target.value)}
            onKeyDown={handleScan}
          />
          <span className="muted small pos-hint">Enter 로 담기</span>
        </div>

        {filteredProducts.length === 0 ? (
          <EmptyState title="맞는 상품이 없습니다." />
        ) : apparel ? (
          <div className="product-grid">
            {filteredProducts.map((product) => (
              <StyleCard key={product.productId} product={product} onOpen={() => setStyleId(product.productId)} />
            ))}
          </div>
        ) : (
          <div className="product-grid">
            {filteredProducts.map((product) => {
              const sku = product.skus.find((candidate) => candidate.sellable)
              if (!sku) return null
              const stock = sku.stockQuantity ?? 0
              return (
                <button
                  key={sku.skuId}
                  type="button"
                  className={`product-tile ${stock === 0 ? 'is-sold-out' : ''}`}
                  onClick={() =>
                    stock === 0
                      ? openAvailability({ skuId: sku.skuId, itemName: sku.displayName, reason: '이 매장 재고가 없습니다.' })
                      : add(sku)
                  }
                >
                  <span className="tile-name">{product.name}</span>
                  <span className="tile-price">{formatWon(sku.price)}</span>
                  <span className={`tile-stock ${stock <= 0 ? 'is-zero' : ''}`}>
                    {stock === 0 ? '품절 · 다른 매장 보기' : `재고 ${formatNumber(stock)}`}
                  </span>
                </button>
              )
            })}
          </div>
        )}
      </section>

      <aside className="pos-cart" aria-label="장바구니">
        <div className="cart-header">
          <h2>장바구니</h2>
          {cart.length > 0 && (
            <button type="button" className="link small" onClick={() => setCart([])}>
              비우기
            </button>
          )}
        </div>
        {lastPaid && cart.length === 0 && (
          <div className="receipt-banner" role="status">
            주문 #{lastPaid.orderId} · {lastPaid.paymentMethod?.description} {formatWon(lastPaid.totalAmount)} 결제 완료
          </div>
        )}
        {cart.length === 0 ? (
          <EmptyState title="담긴 상품이 없습니다.">{apparel ? '스타일을 눌러 색상·사이즈를 고르세요.' : '바코드를 찍거나 상품을 누르세요.'}</EmptyState>
        ) : (
          <ul className="cart-lines">
            {cart.map((line) => {
              const over = line.sku.stockQuantity !== null && line.quantity > line.sku.stockQuantity
              return (
                <li key={line.sku.skuId}>
                  <div className="grow">
                    <span className="line-name">{line.sku.displayName}</span>
                    <span className="muted small">{formatWon(line.sku.price)}</span>
                    {over && <span className="warning-text small">재고 {formatNumber(line.sku.stockQuantity ?? 0)}개보다 많습니다</span>}
                  </div>
                  <QuantityStepper
                    value={line.quantity}
                    label={line.sku.displayName}
                    onChange={(quantity) => changeQuantity(line.sku.skuId, quantity)}
                  />
                  <strong className="line-amount">{formatWon(line.sku.price * line.quantity)}</strong>
                </li>
              )
            })}
          </ul>
        )}
        <div className="cart-footer">
          <div className="cart-total">
            <span>
              합계 <span className="muted small">{formatNumber(itemCount)}개</span>
            </span>
            <strong>{formatWon(total)}</strong>
          </div>
          <button type="button" className="button large block" disabled={cart.length === 0} onClick={() => setPayOpen(true)}>
            결제하기
          </button>
        </div>
      </aside>

      {style && (
        <ApparelMatrixDialog
          product={style}
          cart={new Map(cart.map((line) => [line.sku.skuId, line.quantity]))}
          onAdd={add}
          onSoldOut={(sku) =>
            openAvailability({ skuId: sku.skuId, itemName: sku.displayName, reason: `${sku.displayName}의 이 매장 재고가 없습니다.` })
          }
          onClose={() => setStyleId(null)}
        />
      )}
      {payOpen && <PaymentDialog total={total} busy={paying} onClose={() => setPayOpen(false)} onPay={(method) => void checkout(method)} />}
    </div>
  )
}

function StyleCard({ product, onOpen }: { product: CatalogProduct; onOpen: () => void }) {
  const sellable = product.skus.filter((sku) => sku.sellable)
  const prices = sellable.map((sku) => sku.price)
  const min = Math.min(...prices)
  const max = Math.max(...prices)
  const stock = sellable.reduce((sum, sku) => sum + (sku.stockQuantity ?? 0), 0)
  const colors = new Set(sellable.map((sku) => sku.color))

  return (
    <button type="button" className={`product-tile ${stock === 0 ? 'is-sold-out' : ''}`} onClick={onOpen}>
      <span className="tile-name">{product.name}</span>
      <span className="tile-price">{min === max ? formatWon(min) : `${formatWon(min)}~`}</span>
      <span className="tile-stock">
        {colors.size}색 · {sellable.length}개 옵션 · 재고 {formatNumber(stock)}
      </span>
    </button>
  )
}

export function QuantityStepper({ value, label, onChange, disabled }: { value: number; label: string; onChange: (value: number) => void; disabled?: boolean }) {
  return (
    <div className="stepper" role="group" aria-label={`${label} 수량`}>
      <button type="button" aria-label="하나 빼기" disabled={disabled} onClick={() => onChange(value - 1)}>
        −
      </button>
      <span aria-live="polite">{value}</span>
      <button type="button" aria-label="하나 더하기" disabled={disabled} onClick={() => onChange(value + 1)}>
        +
      </button>
    </div>
  )
}
