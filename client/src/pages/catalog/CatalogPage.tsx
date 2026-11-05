import { useCallback, useState } from 'react'
import { catalogApi } from '../../shared/api/catalog/catalogService'
import type { CatalogProduct } from '../../shared/api/catalog/catalogService'
import { useResource } from '../../shared/hooks/useResource'
import { isApparel, useCurrentStore } from '../../shared/store/storeContext'
import { Badge } from '../../shared/ui/Badge'
import { EmptyState, LoadState } from '../../shared/ui/EmptyState'
import { IconSearch } from '../../shared/ui/icons'
import { formatNumber, formatQuantity, formatWon } from '../../shared/utils/format'
import { compareSize } from '../pos/posUtils'
import './catalog.css'

/** SCR-09 상품 (FR-CA-001). 브랜드 상품 마스터와 SKU, 이 매장 재고 */
export default function CatalogPage() {
  const { store } = useCurrentStore()
  const loader = useCallback(() => catalogApi.list(store.storeId), [store.storeId])
  const { data, loading, error, reload } = useResource(loader)
  const [query, setQuery] = useState('')
  const [kind, setKind] = useState('ALL')

  const keyword = query.trim().toLowerCase()
  const kinds = [...new Map((data ?? []).map((product) => [product.kind.code, product.kind])).values()]
  const rows = (data ?? []).filter(
    (product) =>
      (kind === 'ALL' || product.kind.code === kind) &&
      (!keyword ||
        product.name.toLowerCase().includes(keyword) ||
        product.skus.some((sku) => sku.skuCode.toLowerCase().includes(keyword) || sku.barcode === query.trim())),
  )

  return (
    <div className="page-content">
      <header className="page-header">
        <div>
          <h1>상품</h1>
          <p className="muted">
            {store.brandName} 상품 {formatNumber(data?.length ?? 0)}개 · 가격은 브랜드 단일가
          </p>
        </div>
      </header>

      <div className="toolbar">
        <div className="search-field">
          <IconSearch />
          <label className="visually-hidden" htmlFor="catalog-search">
            상품 검색
          </label>
          <input id="catalog-search" type="search" value={query} placeholder="상품 이름·SKU·바코드" onChange={(event) => setQuery(event.target.value)} />
        </div>
        {kinds.length > 1 && (
          <div className="chip-row" role="tablist" aria-label="상품 종류">
            <button type="button" role="tab" aria-selected={kind === 'ALL'} className={`chip ${kind === 'ALL' ? 'selected' : ''}`} onClick={() => setKind('ALL')}>
              전체
            </button>
            {kinds.map((item) => (
              <button
                key={item.code}
                type="button"
                role="tab"
                aria-selected={kind === item.code}
                className={`chip ${kind === item.code ? 'selected' : ''}`}
                onClick={() => setKind(item.code)}
              >
                {item.description}
              </button>
            ))}
          </div>
        )}
      </div>

      {!data && <LoadState loading={loading} error={error} onRetry={reload} />}
      {data && rows.length === 0 && <EmptyState title="상품이 없습니다." />}
      <div className="catalog-list">
        {rows.map((product) => (
          <ProductCard key={product.productId} product={product} apparel={isApparel(store)} />
        ))}
      </div>
    </div>
  )
}

function ProductCard({ product, apparel }: { product: CatalogProduct; apparel: boolean }) {
  const statusTone = product.status.code === 'ON_SALE' ? 'success' : product.status.code === 'SUSPENDED' ? 'warning' : 'neutral'

  return (
    <article className="card product-card">
      <header className="card-header">
        <div>
          <h2>{product.name}</h2>
          <span className="muted small">{product.category ?? '분류 없음'}</span>
        </div>
        <div className="badge-row">
          <Badge tone="info">{product.kind.description}</Badge>
          <Badge tone={statusTone}>{product.status.description}</Badge>
        </div>
      </header>
      {apparel && product.skus.length > 1 ? <ApparelGrid product={product} /> : <SkuTable product={product} />}
    </article>
  )
}

function SkuTable({ product }: { product: CatalogProduct }) {
  return (
    <table className="data-table compact responsive">
      <thead>
        <tr>
          <th scope="col">SKU</th>
          <th scope="col">바코드</th>
          <th scope="col" className="num">가격</th>
          <th scope="col" className="num">이 매장 재고</th>
        </tr>
      </thead>
      <tbody>
        {product.skus.map((sku) => (
          <tr key={sku.skuId}>
            <td data-label="SKU">{sku.skuCode}</td>
            <td data-label="바코드" className="muted">
              {sku.barcode ?? '-'}
            </td>
            <td data-label="가격" className="num">
              {product.kind.code === 'INGREDIENT' ? <span className="muted">판매 안 함</span> : formatWon(sku.price)}
            </td>
            <td data-label="이 매장 재고" className="num">
              {sku.stockTracked ? formatQuantity(sku.stockQuantity ?? 0, sku.unit) : <span className="muted">레시피로 차감</span>}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function ApparelGrid({ product }: { product: CatalogProduct }) {
  const colors = [...new Set(product.skus.map((sku) => sku.color ?? '-'))]
  const sizes = [...new Set(product.skus.map((sku) => sku.size ?? '-'))].sort(compareSize)
  const prices = [...new Set(product.skus.map((sku) => sku.price))]

  return (
    <>
      <p className="small">{prices.map(formatWon).join(' / ')}</p>
      <div className="matrix-scroll">
        <table className="size-matrix readonly">
          <thead>
            <tr>
              <th scope="col">색상</th>
              {sizes.map((size) => (
                <th key={size} scope="col">
                  {size}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {colors.map((color) => (
              <tr key={color}>
                <th scope="row">{color}</th>
                {sizes.map((size) => {
                  const sku = product.skus.find((candidate) => (candidate.color ?? '-') === color && (candidate.size ?? '-') === size)
                  const stock = sku?.stockQuantity ?? 0
                  return (
                    <td key={size} className={!sku ? 'matrix-empty' : stock === 0 ? 'is-zero' : ''} title={sku?.skuCode}>
                      {sku ? formatNumber(stock) : '-'}
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}
