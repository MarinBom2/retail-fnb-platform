import { useCallback, useState } from 'react'
import { hqApi } from '../../shared/api/hq/hqService'
import type { HqProduct, HqSku, ProductStatusAction } from '../../shared/api/hq/hqService'
import { isApparelBrand, isFoodBrand, useCurrentBrand } from '../../shared/hq/hqContext'
import { useResource } from '../../shared/hooks/useResource'
import { Badge } from '../../shared/ui/Badge'
import type { BadgeTone } from '../../shared/ui/badgeTone'
import { useConfirm } from '../../shared/ui/confirmContext'
import { EmptyState, LoadState } from '../../shared/ui/EmptyState'
import { IconChevronRight, IconPlus, IconRefresh, IconSearch } from '../../shared/ui/icons'
import { Menu } from '../../shared/ui/Menu'
import type { MenuItem } from '../../shared/ui/Menu'
import { Segmented } from '../../shared/ui/Segmented'
import { useToast } from '../../shared/ui/toastContext'
import { formatNumber, formatWon } from '../../shared/utils/format'
import { ProductCreateDialog } from './ProductCreateDialog'
import { RecipeDialog } from './RecipeDialog'
import { AddApparelSkuDialog, PriceDialog } from './SkuDialogs'
import './hq.css'

type KindFilter = 'ALL' | 'MENU' | 'INGREDIENT'

const STATUS_TEXT: Record<ProductStatusAction, { done: string; fail: string }> = {
  suspend: { done: '판매를 중지했습니다. 모든 매장 POS 에서 빠집니다.', fail: '판매를 중지하지 못했습니다.' },
  resume: { done: '판매를 다시 시작했습니다.', fail: '판매를 다시 시작하지 못했습니다.' },
  discontinue: { done: '단종했습니다.', fail: '단종하지 못했습니다.' },
}

function statusTone(code: string): BadgeTone {
  return code === 'ON_SALE' ? 'success' : code === 'SUSPENDED' ? 'warning' : 'neutral'
}

/** SCR-12 본사 상품 관리: 등록·판매 상태·가격·색상/사이즈 추가·레시피 (FR-CA-001~004) */
export default function BrandProductsPage() {
  const { brand, reloadBrands } = useCurrentBrand()
  const apparel = isApparelBrand(brand)
  const food = isFoodBrand(brand)
  const toast = useToast()
  const confirm = useConfirm()
  const loader = useCallback(() => hqApi.products(brand.brandId), [brand.brandId])
  const { data, loading, error, reload, replace } = useResource(loader)

  const [query, setQuery] = useState('')
  const [kind, setKind] = useState<KindFilter>('ALL')
  const [expanded, setExpanded] = useState<Set<number>>(new Set())
  const [creating, setCreating] = useState(false)
  const [pricing, setPricing] = useState<HqProduct | null>(null)
  const [addingSku, setAddingSku] = useState<HqProduct | null>(null)
  const [recipeFor, setRecipeFor] = useState<HqSku | null>(null)

  const products = data ?? []
  const allSkuCodes = products.flatMap((p) => p.skus.map((sku) => sku.skuCode))
  const categories = [...new Set(products.map((p) => p.category).filter((c): c is string => !!c))]
  const ingredients = products.filter((p) => p.kind.code === 'INGREDIENT' && p.status.code !== 'DISCONTINUED').flatMap((p) => p.skus)

  const keyword = query.trim().toLowerCase()
  const rows = products.filter(
    (p) =>
      (kind === 'ALL' || p.kind.code === kind) &&
      (!keyword ||
        p.name.toLowerCase().includes(keyword) ||
        p.skus.some((sku) => sku.skuCode.toLowerCase().includes(keyword) || (sku.barcode ?? '').includes(keyword))),
  )
  const groups = groupByCategory(rows)

  const updateProduct = (updated: HqProduct) => replace(products.map((p) => (p.productId === updated.productId ? updated : p)))
  const updateSkus = (skus: HqSku[]) =>
    replace(products.map((p) => ({ ...p, skus: p.skus.map((sku) => skus.find((s) => s.skuId === sku.skuId) ?? sku) })))

  const toggle = (productId: number) =>
    setExpanded((set) => {
      const next = new Set(set)
      if (next.has(productId)) next.delete(productId)
      else next.add(productId)
      return next
    })

  const changeStatus = async (product: HqProduct, action: ProductStatusAction) => {
    if (action !== 'resume') {
      const ok = await confirm(
        action === 'discontinue'
          ? {
              title: `${product.name}을(를) 단종할까요?`,
              message: '단종은 되돌릴 수 없습니다. 모든 매장 POS 에서 빠지고 색상·사이즈도 더할 수 없습니다. 재고 기록은 남습니다.',
              confirmLabel: '단종',
              tone: 'danger',
            }
          : {
              title: `${product.name} 판매를 중지할까요?`,
              message: '모든 매장 POS 에서 잠시 빠집니다. 언제든 다시 시작할 수 있습니다.',
              confirmLabel: '판매 중지',
            },
      )
      if (!ok) return
    }
    try {
      updateProduct(await hqApi.changeProductStatus(brand.brandId, product.productId, action))
      toast.show(STATUS_TEXT[action].done, 'success')
    } catch (caught) {
      toast.error(caught, STATUS_TEXT[action].fail)
    }
  }

  const menuItems = (product: HqProduct): MenuItem[] => {
    const discontinued = product.status.code === 'DISCONTINUED'
    const items: MenuItem[] = []
    if (product.kind.code === 'MENU') {
      items.push({ label: '레시피', onSelect: () => setRecipeFor(product.skus[0]) })
    }
    if (product.kind.code !== 'INGREDIENT' && !discontinued) {
      items.push({ label: '가격 바꾸기', onSelect: () => setPricing(product) })
    }
    if (apparel && !discontinued) {
      items.push({ label: '색상·사이즈 추가', onSelect: () => setAddingSku(product) })
    }
    if (product.status.code === 'ON_SALE') {
      items.push({ label: '판매 중지', onSelect: () => void changeStatus(product, 'suspend') })
    }
    if (product.status.code === 'SUSPENDED') {
      items.push({ label: '판매 재개', onSelect: () => void changeStatus(product, 'resume') })
    }
    if (!discontinued) {
      items.push({ label: '단종', onSelect: () => void changeStatus(product, 'discontinue'), danger: true })
    }
    return items
  }

  const createLabel = apparel ? '스타일 등록' : food ? '메뉴·식자재 등록' : '상품 등록'

  return (
    <div className="page-content">
      <header className="page-header">
        <div>
          <h1>상품 관리</h1>
          <p className="muted">
            {brand.name} · 상품 {formatNumber(products.length)}개 · SKU {formatNumber(allSkuCodes.length)}개 · 모든 매장에 같은 가격
          </p>
        </div>
        <div className="header-actions">
          <button type="button" className="button secondary small" onClick={reload} disabled={loading}>
            <IconRefresh className="button-icon" />
            새로고침
          </button>
          <button type="button" className="button small" onClick={() => setCreating(true)}>
            <IconPlus className="button-icon" />
            {createLabel}
          </button>
        </div>
      </header>

      <div className="toolbar">
        <div className="search-field">
          <IconSearch />
          <label className="visually-hidden" htmlFor="product-search">
            상품 검색
          </label>
          <input
            id="product-search"
            type="search"
            placeholder="이름·SKU 코드·바코드"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </div>
        {food && (
          <Segmented<KindFilter>
            label="종류"
            value={kind}
            onChange={setKind}
            options={[
              { value: 'ALL', label: '전체' },
              { value: 'MENU', label: '메뉴' },
              { value: 'INGREDIENT', label: '식자재' },
            ]}
          />
        )}
      </div>

      {!data && <LoadState loading={loading} error={error} onRetry={reload} />}

      {data && products.length === 0 && (
        <EmptyState
          title="아직 상품이 없습니다."
          action={
            <button type="button" className="button" onClick={() => setCreating(true)}>
              {createLabel}
            </button>
          }
        >
          {apparel
            ? '스타일 하나에 색상과 사이즈를 고르면 SKU 가 한 번에 만들어집니다.'
            : food
              ? '식자재를 먼저 등록하면 메뉴를 만들 때 레시피를 바로 넣을 수 있습니다.'
              : '바코드가 있으면 POS 에서 스캔으로 팔 수 있습니다.'}
        </EmptyState>
      )}

      {data && products.length > 0 && rows.length === 0 && <EmptyState title="찾는 상품이 없습니다." />}

      {groups.map(([category, list]) => (
        <section key={category} className="category-group" aria-label={category}>
          <h2>{category}</h2>
          <ul className="product-list">
            {list.map((product) => {
              const open = expanded.has(product.productId)
              const items = menuItems(product)
              return (
                <li key={product.productId}>
                  <div className={`product-row ${product.status.code === 'DISCONTINUED' ? 'is-discontinued' : ''}`}>
                    <button
                      type="button"
                      className="icon-button expand-button"
                      aria-expanded={open}
                      aria-label={`${product.name} SKU ${open ? '접기' : '펼치기'}`}
                      onClick={() => toggle(product.productId)}
                    >
                      <IconChevronRight />
                    </button>
                    <span className="hq-name">
                      <strong>{product.name}</strong>
                      <span className="small">{productSummary(product)}</span>
                    </span>
                    <span className="product-badges">
                      {food && <Badge tone={product.kind.code === 'MENU' ? 'food' : 'neutral'}>{product.kind.description}</Badge>}
                      {product.kind.code === 'MENU' && !product.skus[0]?.hasRecipe && <Badge tone="warning">레시피 없음</Badge>}
                      <Badge tone={statusTone(product.status.code)}>{product.status.description}</Badge>
                    </span>
                    {product.kind.code !== 'INGREDIENT' && <span className="product-price">{priceRange(product)}</span>}
                    {items.length > 0 && <Menu label={`${product.name} 더 보기`} items={items} />}
                  </div>
                  {open && <SkuTable product={product} />}
                </li>
              )
            })}
          </ul>
        </section>
      ))}

      {creating && (
        <ProductCreateDialog
          existingSkuCodes={allSkuCodes}
          categories={categories}
          onClose={() => setCreating(false)}
          onCreated={async (product) => {
            setCreating(false)
            replace([...products, product])
            reloadBrands()
            if (product.kind.code === 'MENU') {
              const now = await confirm({
                title: '레시피를 바로 등록할까요?',
                message: '레시피가 있어야 메뉴가 팔릴 때 식자재 재고가 빠집니다. 나중에 등록해도 됩니다.',
                confirmLabel: '레시피 등록',
              })
              if (now) setRecipeFor(product.skus[0])
            }
          }}
        />
      )}
      {pricing && (
        <PriceDialog
          key={pricing.productId}
          product={pricing}
          onClose={() => setPricing(null)}
          onSaved={(skus) => {
            updateSkus(skus)
            setPricing(null)
          }}
        />
      )}
      {addingSku && (
        <AddApparelSkuDialog
          key={addingSku.productId}
          product={addingSku}
          existingSkuCodes={allSkuCodes}
          onClose={() => setAddingSku(null)}
          onSaved={(updated) => {
            updateProduct(updated)
            setAddingSku(null)
          }}
        />
      )}
      {recipeFor && (
        <RecipeDialog
          key={recipeFor.skuId}
          menu={recipeFor}
          ingredients={ingredients}
          onClose={() => setRecipeFor(null)}
          onSaved={(recipe) => {
            updateSkus(
              products.flatMap((p) => p.skus).filter((sku) => sku.skuId === recipe.menuSkuId).map((sku) => ({ ...sku, hasRecipe: recipe.lines.length > 0 })),
            )
            setRecipeFor(null)
          }}
        />
      )}
    </div>
  )
}

function SkuTable({ product }: { product: HqProduct }) {
  const apparel = product.skus.some((sku) => sku.color)
  const ingredient = product.kind.code === 'INGREDIENT'
  return (
    <div className="sku-table">
      <div className="table-wrap">
        <table className="data-table compact">
          <thead>
            <tr>
              <th scope="col">SKU 코드</th>
              {apparel && <th scope="col">색상 / 사이즈</th>}
              {!apparel && !ingredient && <th scope="col">바코드</th>}
              {ingredient ? <th scope="col">단위</th> : <th scope="col" className="num">가격</th>}
            </tr>
          </thead>
          <tbody>
            {product.skus.map((sku) => (
              <tr key={sku.skuId}>
                <td>
                  <code>{sku.skuCode}</code>
                </td>
                {apparel && (
                  <td>
                    {sku.color} / {sku.size}
                  </td>
                )}
                {!apparel && !ingredient && <td className="muted">{sku.barcode ?? '-'}</td>}
                {ingredient ? <td>{sku.unit.description}</td> : <td className="num">{formatWon(sku.price)}</td>}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

/** 의류: "BLACK·WHITE · S~XL · SKU 8개", 그 밖: SKU 코드 (+ 단위) */
function productSummary(product: HqProduct) {
  if (product.skus.some((sku) => sku.color)) {
    const colors = [...new Set(product.skus.map((sku) => sku.color))]
    const sizes = [...new Set(product.skus.map((sku) => sku.size))]
    const sizeText = sizes.length > 2 ? `${sizes[0]}~${sizes[sizes.length - 1]}` : sizes.join('·')
    return `${colors.join('·')} · ${sizeText} · SKU ${product.skus.length}개`
  }
  const sku = product.skus[0]
  if (!sku) return 'SKU 없음'
  return product.kind.code === 'INGREDIENT' ? `${sku.skuCode} · 단위 ${sku.unit.description}` : sku.skuCode
}

function priceRange(product: HqProduct) {
  const prices = product.skus.map((sku) => sku.price)
  if (prices.length === 0) return ''
  const min = Math.min(...prices)
  const max = Math.max(...prices)
  return min === max ? formatWon(min) : `${formatWon(min)}~${formatWon(max)}`
}

function groupByCategory(products: HqProduct[]): [string, HqProduct[]][] {
  const map = new Map<string, HqProduct[]>()
  for (const product of products) {
    const key = product.kind.code === 'INGREDIENT' ? '식자재' : (product.category ?? '분류 없음')
    map.set(key, [...(map.get(key) ?? []), product])
  }
  return [...map.entries()].sort(([a], [b]) => a.localeCompare(b, 'ko'))
}
