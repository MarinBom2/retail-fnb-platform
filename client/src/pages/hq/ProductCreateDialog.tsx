import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { hqApi } from '../../shared/api/hq/hqService'
import type { HqProduct, NewSku } from '../../shared/api/hq/hqService'
import { isApparelBrand, isFoodBrand, useCurrentBrand } from '../../shared/hq/hqContext'
import { Dialog } from '../../shared/ui/Dialog'
import { Segmented } from '../../shared/ui/Segmented'
import { useToast } from '../../shared/ui/toastContext'
import { formatNumber } from '../../shared/utils/format'
import { ChipInput } from './ChipInput'
import { codePart, hqErrorMessage, nextCode } from './hqUtils'

type FoodKind = 'MENU' | 'INGREDIENT'
type UnitCode = 'EA' | 'GRAM' | 'MILLILITER'

const SIZE_PRESETS: { label: string; sizes: string[] }[] = [
  { label: 'S M L XL', sizes: ['S', 'M', 'L', 'XL'] },
  { label: '28 30 32 34', sizes: ['28', '30', '32', '34'] },
  { label: 'FREE', sizes: ['FREE'] },
]

const DUPLICATE = { ALREADY_EXIST: '이미 쓰는 SKU 코드·바코드입니다. 코드를 바꿔 주세요.' }

/**
 * 상품 등록 (FR-CA-001). 업종마다 폼이 다르다.
 * 생활용품: 바코드 SKU 하나 / 의류: 색상 × 사이즈 SKU 를 한 번에 / 외식: 메뉴(가격) 또는 식자재(단위)
 */
export function ProductCreateDialog({
  existingSkuCodes,
  categories,
  onClose,
  onCreated,
}: {
  existingSkuCodes: string[]
  categories: string[]
  onClose: () => void
  /** 외식 메뉴면 레시피를 바로 등록할지 묻는다 */
  onCreated: (product: HqProduct) => void
}) {
  const { brand } = useCurrentBrand()
  const apparel = isApparelBrand(brand)
  const food = isFoodBrand(brand)
  const toast = useToast()

  const [foodKind, setFoodKind] = useState<FoodKind>('MENU')
  const [name, setName] = useState('')
  const [category, setCategory] = useState('')
  const [price, setPrice] = useState('')
  const [barcode, setBarcode] = useState('')
  const [unit, setUnit] = useState<UnitCode>('GRAM')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 단일 SKU 코드 제안값: 생활용품 SKU-001, 외식 메뉴 MN-001, 식자재 IG-001
  const singlePrefix = food ? (foodKind === 'MENU' ? 'MN' : 'IG') : 'SKU'
  const suggested = useMemo(() => nextCode(singlePrefix, existingSkuCodes), [singlePrefix, existingSkuCodes])
  const [skuCode, setSkuCode] = useState<string | null>(null)
  const singleCode = skuCode ?? suggested

  // 의류: 스타일 코드 + 색상 × 사이즈
  const [stylePrefix, setStylePrefix] = useState(() => nextCode('ST', existingSkuCodes.map((c) => c.split('-').slice(0, 2).join('-'))))
  const [colors, setColors] = useState<string[]>([])
  const [sizes, setSizes] = useState<string[]>([])
  const apparelSkus = colors.flatMap((color) =>
    sizes.map((size) => ({ color, size, skuCode: `${codePart(stylePrefix)}-${codePart(color)}-${codePart(size)}` })),
  )
  const duplicateCodes = apparelSkus.filter((sku) => existingSkuCodes.includes(sku.skuCode)).map((sku) => sku.skuCode)

  const isIngredient = food && foodKind === 'INGREDIENT'
  const priceNumber = Number(price)
  const priceValid = isIngredient || (price !== '' && Number.isInteger(priceNumber) && priceNumber >= 0)
  const valid =
    name.trim().length > 0 &&
    priceValid &&
    (apparel ? apparelSkus.length > 0 && duplicateCodes.length === 0 && stylePrefix.trim().length > 0 : singleCode.trim().length > 0)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!valid) return
    const skus: NewSku[] = apparel
      ? apparelSkus.map((sku) => ({ skuCode: sku.skuCode, price: priceNumber, color: sku.color, size: sku.size, unit: 'EA' }))
      : [
          {
            skuCode: singleCode.trim(),
            barcode: !food && barcode.trim() ? barcode.trim() : null,
            price: isIngredient ? null : priceNumber,
            unit: isIngredient ? unit : 'EA',
          },
        ]
    setBusy(true)
    setError(null)
    try {
      const product = await hqApi.createProduct(brand.brandId, {
        kind: food ? foodKind : 'MERCHANDISE',
        name: name.trim(),
        category: category.trim() || null,
        skus,
      })
      toast.show(
        apparel ? `${product.name} SKU ${formatNumber(product.skus.length)}개를 만들었습니다.` : `${product.name}을(를) 등록했습니다.`,
        'success',
      )
      onCreated(product)
    } catch (caught) {
      setError(hqErrorMessage(caught, '등록하지 못했습니다.', DUPLICATE))
    } finally {
      setBusy(false)
    }
  }

  const title = apparel ? '스타일 등록' : food ? (foodKind === 'MENU' ? '메뉴 등록' : '식자재 등록') : '상품 등록'

  return (
    <Dialog open title={title} description={brand.businessType.description} size={apparel ? 'lg' : 'md'} onClose={onClose}>
      <form className="form sticky-actions" onSubmit={submit}>
        {food && (
          <Segmented<FoodKind>
            label="종류"
            value={foodKind}
            onChange={(value) => {
              setFoodKind(value)
              setSkuCode(null)
            }}
            options={[
              { value: 'MENU', label: '메뉴' },
              { value: 'INGREDIENT', label: '식자재' },
            ]}
            block
          />
        )}

        <div className="form-row">
          <label className="grow">
            {apparel ? '스타일 이름' : '이름'}
            <input
              value={name}
              maxLength={200}
              required
              placeholder={apparel ? '예: 크루넥 반팔 티셔츠' : food ? (isIngredient ? '예: 쌀' : '예: 김치찌개') : '예: 스테인리스 머그컵'}
              onChange={(event) => setName(event.target.value)}
            />
          </label>
          {!isIngredient && (
            <label>
              분류
              <input value={category} maxLength={50} list="category-options" placeholder="예: 주방" onChange={(event) => setCategory(event.target.value)} />
              <datalist id="category-options">
                {categories.map((c) => (
                  <option key={c} value={c} />
                ))}
              </datalist>
            </label>
          )}
        </div>

        {!isIngredient && (
          <label>
            판매가 (원){apparel && ' · 모든 SKU 같은 가격'}
            <input
              type="number"
              inputMode="numeric"
              min={0}
              step={1}
              required
              value={price}
              placeholder="0"
              onChange={(event) => setPrice(event.target.value)}
            />
          </label>
        )}

        {isIngredient && (
          <fieldset className="choice-group">
            <legend>재고 단위</legend>
            <Segmented<UnitCode>
              label="재고 단위"
              value={unit}
              onChange={setUnit}
              options={[
                { value: 'EA', label: '개' },
                { value: 'GRAM', label: 'g' },
                { value: 'MILLILITER', label: 'ml' },
              ]}
            />
            <span className="muted small">레시피 양과 입고 수량을 이 단위의 정수로 셉니다. 식자재는 판매가가 없습니다.</span>
          </fieldset>
        )}

        {!apparel && (
          <div className="form-row">
            <label>
              SKU 코드
              <input value={singleCode} maxLength={50} required onChange={(event) => setSkuCode(event.target.value)} />
            </label>
            {!food && (
              <label>
                바코드 (선택)
                <input value={barcode} maxLength={50} inputMode="numeric" placeholder="예: 8801234567890" onChange={(event) => setBarcode(event.target.value)} />
              </label>
            )}
          </div>
        )}

        {apparel && (
          <>
            <label>
              스타일 코드 (SKU 코드 앞부분)
              <input value={stylePrefix} maxLength={20} required onChange={(event) => setStylePrefix(event.target.value)} />
            </label>
            <div className="form-field">
              <label htmlFor="color-input" className="field-label">
                색상 <span className="muted small">입력하고 Enter</span>
              </label>
              <ChipInput id="color-input" values={colors} onChange={setColors} placeholder="예: BLACK, WHITE" />
            </div>
            <div className="form-field">
              <label htmlFor="size-input" className="field-label">
                사이즈 <span className="muted small">입력하고 Enter, 또는 빠른 선택</span>
              </label>
              <ChipInput id="size-input" values={sizes} onChange={setSizes} placeholder="예: S, M, L" />
              <div className="preset-row">
                {SIZE_PRESETS.map((preset) => (
                  <button key={preset.label} type="button" className="chip" onClick={() => setSizes(preset.sizes)}>
                    {preset.label}
                  </button>
                ))}
              </div>
            </div>
            <div className="sku-preview" aria-live="polite">
              <div className="sku-preview-head">
                <strong>{apparelSkus.length > 0 ? `${formatNumber(apparelSkus.length)}개 SKU 생성` : '색상과 사이즈를 넣으면 SKU 가 만들어집니다'}</strong>
                {apparelSkus.length > 0 && (
                  <span className="muted small">
                    색상 {colors.length} × 사이즈 {sizes.length}
                  </span>
                )}
              </div>
              {apparelSkus.length > 0 && (
                <div className="sku-preview-grid">
                  {apparelSkus.map((sku) => (
                    <code key={sku.skuCode} title={sku.skuCode} className={duplicateCodes.includes(sku.skuCode) ? 'danger-text' : ''}>
                      {sku.skuCode}
                    </code>
                  ))}
                </div>
              )}
              {duplicateCodes.length > 0 && (
                <p className="field-error">이미 쓰는 SKU 코드가 있습니다. 스타일 코드를 바꿔 주세요.</p>
              )}
            </div>
          </>
        )}

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
            {busy ? '등록 중…' : apparel && apparelSkus.length > 0 ? `SKU ${formatNumber(apparelSkus.length)}개 등록` : '등록'}
          </button>
        </div>
      </form>
    </Dialog>
  )
}
