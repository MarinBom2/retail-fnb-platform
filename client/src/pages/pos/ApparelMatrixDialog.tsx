import type { CatalogProduct, CatalogSku } from '../../shared/api/catalog/catalogService'
import { Dialog } from '../../shared/ui/Dialog'
import { formatNumber, formatWon } from '../../shared/utils/format'
import { compareSize } from './posUtils'

type Props = {
  product: CatalogProduct
  /** skuId → 장바구니 수량 */
  cart: Map<number, number>
  onAdd: (sku: CatalogSku) => void
  onSoldOut: (sku: CatalogSku) => void
  onClose: () => void
}

/** 의류 색상 × 사이즈 표. 칸마다 이 매장 재고를 보여 주고, 품절이면 다른 매장 재고로 잇는다 */
export function ApparelMatrixDialog({ product, cart, onAdd, onSoldOut, onClose }: Props) {
  const skus = product.skus.filter((sku) => sku.sellable)
  const colors = [...new Set(skus.map((sku) => sku.color ?? '-'))]
  const sizes = [...new Set(skus.map((sku) => sku.size ?? '-'))].sort(compareSize)
  const cell = (color: string, size: string) => skus.find((sku) => (sku.color ?? '-') === color && (sku.size ?? '-') === size)

  return (
    <Dialog open title={product.name} description="색상·사이즈를 눌러 담으세요. 숫자는 이 매장 재고입니다." size="lg" onClose={onClose}>
      <div className="matrix-scroll">
        <table className="size-matrix">
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
                  const sku = cell(color, size)
                  if (!sku) return <td key={size} className="matrix-empty">-</td>
                  const stock = sku.stockQuantity ?? 0
                  const inCart = cart.get(sku.skuId) ?? 0
                  return (
                    <td key={size}>
                      <button
                        type="button"
                        className={`matrix-cell ${stock === 0 ? 'is-sold-out' : ''} ${inCart > 0 ? 'in-cart' : ''}`}
                        aria-label={`${color} ${size} 재고 ${stock}${inCart ? `, 담은 수량 ${inCart}` : ''}`}
                        onClick={() => (stock === 0 ? onSoldOut(sku) : onAdd(sku))}
                      >
                        <span className="cell-stock">{stock === 0 ? '품절' : formatNumber(stock)}</span>
                        {inCart > 0 && <span className="cell-cart">+{inCart}</span>}
                      </button>
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="muted small">
        {formatWon(skus[0]?.price ?? 0)} · 품절 칸을 누르면 다른 매장 재고를 보고 이동을 요청할 수 있습니다.
      </p>
      <div className="dialog-actions">
        <button type="button" className="button" data-autofocus onClick={onClose}>
          완료
        </button>
      </div>
    </Dialog>
  )
}
