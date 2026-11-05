import { useState } from 'react'
import type { FormEvent } from 'react'
import { hqApi } from '../../shared/api/hq/hqService'
import type { HqProduct, HqSku } from '../../shared/api/hq/hqService'
import { useCurrentBrand } from '../../shared/hq/hqContext'
import { Dialog } from '../../shared/ui/Dialog'
import { useToast } from '../../shared/ui/toastContext'
import { formatWon } from '../../shared/utils/format'
import { codePart, hqErrorMessage, normalizeOption } from './hqUtils'

/**
 * 가격 바꾸기 (FR-CA-004). API 는 SKU 하나씩이라, 의류처럼 SKU 가 여럿이면 "모든 SKU 같은 가격"으로 차례로 보낸다.
 * 이미 담긴 주문은 담은 순간의 가격을 그대로 쓴다
 */
export function PriceDialog({
  product,
  onClose,
  onSaved,
}: {
  product: HqProduct
  onClose: () => void
  onSaved: (skus: HqSku[]) => void
}) {
  const { brand } = useCurrentBrand()
  const toast = useToast()
  const multiple = product.skus.length > 1
  const [target, setTarget] = useState<'ALL' | number>(multiple ? 'ALL' : product.skus[0].skuId)
  const current = target === 'ALL' ? product.skus[0] : product.skus.find((sku) => sku.skuId === target)!
  const [price, setPrice] = useState(String(current.price))
  const [busy, setBusy] = useState(false)
  const amount = Number(price)
  const valid = price !== '' && Number.isInteger(amount) && amount >= 0

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!valid) return
    setBusy(true)
    try {
      const targets = target === 'ALL' ? product.skus : product.skus.filter((sku) => sku.skuId === target)
      const updated: HqSku[] = []
      for (const sku of targets) {
        updated.push(await hqApi.changePrice(brand.brandId, sku.skuId, amount))
      }
      toast.show(`가격을 ${formatWon(amount)}(으)로 바꿨습니다.`, 'success')
      onSaved(updated)
    } catch (caught) {
      toast.error(caught, '가격을 바꾸지 못했습니다.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open title="가격 바꾸기" description={`${product.name} · 이미 담긴 주문에는 영향이 없습니다.`} size="sm" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        {multiple && (
          <label>
            적용할 SKU
            <select
              value={target}
              onChange={(event) => {
                const next = event.target.value === 'ALL' ? 'ALL' : Number(event.target.value)
                setTarget(next)
                const sku = next === 'ALL' ? product.skus[0] : product.skus.find((s) => s.skuId === next)
                if (sku) setPrice(String(sku.price))
              }}
            >
              <option value="ALL">모든 SKU ({product.skus.length}개) 같은 가격</option>
              {product.skus.map((sku) => (
                <option key={sku.skuId} value={sku.skuId}>
                  {sku.displayName} · {formatWon(sku.price)}
                </option>
              ))}
            </select>
          </label>
        )}
        <label>
          판매가 (원)
          <input type="number" inputMode="numeric" min={0} step={1} required value={price} onChange={(event) => setPrice(event.target.value)} />
        </label>
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

/** 의류 스타일에 색상·사이즈 SKU 하나 더하기. 가격은 기존 SKU 와 같게 시작한다 */
export function AddApparelSkuDialog({
  product,
  existingSkuCodes,
  onClose,
  onSaved,
}: {
  product: HqProduct
  existingSkuCodes: string[]
  onClose: () => void
  onSaved: (product: HqProduct) => void
}) {
  const { brand } = useCurrentBrand()
  const toast = useToast()
  const prefix = product.skus[0]?.skuCode.split('-').slice(0, 2).join('-') ?? 'ST'
  const [color, setColor] = useState('')
  const [size, setSize] = useState('')
  const [codeEdited, setCodeEdited] = useState<string | null>(null)
  const [price, setPrice] = useState(String(product.skus[0]?.price ?? ''))
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const suggested = color.trim() && size.trim() ? `${prefix}-${codePart(color)}-${codePart(size)}` : ''
  const skuCode = codeEdited ?? suggested
  const exists = product.skus.some((sku) => sku.color === normalizeOption(color) && sku.size === normalizeOption(size))
  const amount = Number(price)
  const valid =
    color.trim() !== '' && size.trim() !== '' && skuCode.trim() !== '' && !exists && price !== '' && Number.isInteger(amount) && amount >= 0

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!valid) return
    setBusy(true)
    setError(null)
    try {
      const updated = await hqApi.addSku(brand.brandId, product.productId, {
        skuCode: skuCode.trim(),
        price: amount,
        color: normalizeOption(color),
        size: normalizeOption(size),
        unit: 'EA',
      })
      toast.show(`${normalizeOption(color)} / ${normalizeOption(size)} 를 추가했습니다.`, 'success')
      onSaved(updated)
    } catch (caught) {
      setError(hqErrorMessage(caught, '추가하지 못했습니다.', { ALREADY_EXIST: '이미 쓰는 SKU 코드·바코드입니다. 코드를 바꿔 주세요.' }))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open title="색상·사이즈 추가" description={product.name} size="sm" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <div className="form-row">
          <label>
            색상
            <input value={color} maxLength={30} required placeholder="예: NAVY" onChange={(event) => setColor(event.target.value)} />
          </label>
          <label>
            사이즈
            <input value={size} maxLength={10} required placeholder="예: XL" onChange={(event) => setSize(event.target.value)} />
          </label>
        </div>
        {exists && <p className="field-error">이미 있는 색상·사이즈입니다.</p>}
        <label>
          SKU 코드
          <input
            value={skuCode}
            maxLength={50}
            required
            onChange={(event) => setCodeEdited(event.target.value)}
            aria-invalid={existingSkuCodes.includes(skuCode)}
          />
        </label>
        <label>
          판매가 (원)
          <input type="number" inputMode="numeric" min={0} step={1} required value={price} onChange={(event) => setPrice(event.target.value)} />
        </label>
        {error && (
          <p className="field-error" role="alert">
            {error}
          </p>
        )}
        <div className="dialog-actions">
          <button type="button" className="button secondary" onClick={onClose}>
            닫기
          </button>
          <button type="submit" className="button" disabled={!valid || busy}>
            {busy ? '추가 중…' : '추가'}
          </button>
        </div>
      </form>
    </Dialog>
  )
}
