import { ApiError } from '../../shared/core/apiClient'
import type { CatalogProduct, CatalogSku } from '../../shared/api/catalog/catalogService'

/** 재고 부족 오류면 모자란 SKU ID, 아니면 null */
export function outOfStockSkuId(caught: unknown): number | null {
  if (caught instanceof ApiError && caught.code === 'OUT_OF_STOCK') {
    const skuId = Number(caught.problem?.skuId)
    return Number.isInteger(skuId) && skuId > 0 ? skuId : null
  }
  return null
}

/** 카탈로그 전체에서 SKU 찾기 (식자재 포함) */
export function findSku(catalog: CatalogProduct[] | undefined, skuId: number): CatalogSku | undefined {
  for (const product of catalog ?? []) {
    const sku = product.skus.find((candidate) => candidate.skuId === skuId)
    if (sku) return sku
  }
  return undefined
}

const SIZE_ORDER = ['XXS', 'XS', 'S', 'M', 'L', 'XL', 'XXL', '2XL', '3XL', 'FREE', 'F']

/** 사이즈 순서: 문자 사이즈 표 순서, 숫자 사이즈는 작은 것부터 */
export function compareSize(a: string, b: string) {
  const ai = SIZE_ORDER.indexOf(a)
  const bi = SIZE_ORDER.indexOf(b)
  if (ai >= 0 && bi >= 0) return ai - bi
  const an = Number(a)
  const bn = Number(b)
  if (!Number.isNaN(an) && !Number.isNaN(bn)) return an - bn
  if (ai >= 0) return -1
  if (bi >= 0) return 1
  return a.localeCompare(b)
}
