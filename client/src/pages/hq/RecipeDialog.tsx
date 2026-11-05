import { useCallback, useState } from 'react'
import type { FormEvent } from 'react'
import { hqApi } from '../../shared/api/hq/hqService'
import type { HqSku, RecipeResponse } from '../../shared/api/hq/hqService'
import { useCurrentBrand } from '../../shared/hq/hqContext'
import { useResource } from '../../shared/hooks/useResource'
import { Dialog } from '../../shared/ui/Dialog'
import { LoadState } from '../../shared/ui/EmptyState'
import { IconPlus, IconTrash } from '../../shared/ui/icons'
import { useToast } from '../../shared/ui/toastContext'
import { hqErrorMessage } from './hqUtils'

type Row = { key: number; ingredientSkuId: number | ''; quantity: string }

/**
 * 메뉴 레시피 (FR-CA-003). 메뉴 1개에 드는 식자재와 양(식자재 단위의 정수).
 * 저장하면 통째로 바뀐다. 이미 결제된 주문의 차감량은 바뀌지 않는다
 */
export function RecipeDialog({
  menu,
  ingredients,
  onClose,
  onSaved,
}: {
  menu: HqSku
  ingredients: HqSku[]
  onClose: () => void
  onSaved: (recipe: RecipeResponse) => void
}) {
  const { brand } = useCurrentBrand()
  const loader = useCallback(() => hqApi.recipe(brand.brandId, menu.skuId), [brand.brandId, menu.skuId])
  const { data, loading, error, reload } = useResource(loader)

  return (
    <Dialog open title="레시피" description={`${menu.displayName} 1개에 드는 식자재`} onClose={onClose}>
      {!data && <LoadState loading={loading} error={error} onRetry={reload} />}
      {data && ingredients.length === 0 && (
        <p className="notice-box">먼저 식자재를 등록해 주세요. 레시피에는 이 브랜드의 식자재만 넣을 수 있습니다.</p>
      )}
      {data && ingredients.length > 0 && (
        <RecipeForm key={data.menuSkuId} menu={menu} initial={data} ingredients={ingredients} onClose={onClose} onSaved={onSaved} />
      )}
    </Dialog>
  )
}

function RecipeForm({
  menu,
  initial,
  ingredients,
  onClose,
  onSaved,
}: {
  menu: HqSku
  initial: RecipeResponse
  ingredients: HqSku[]
  onClose: () => void
  onSaved: (recipe: RecipeResponse) => void
}) {
  const { brand } = useCurrentBrand()
  const toast = useToast()
  const [rows, setRows] = useState<Row[]>(() =>
    initial.lines.length > 0
      ? initial.lines.map((line, i) => ({ key: i, ingredientSkuId: line.ingredientSkuId, quantity: String(line.quantity) }))
      : [{ key: 0, ingredientSkuId: '', quantity: '' }],
  )
  const [nextKey, setNextKey] = useState(rows.length)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const chosen = rows.map((row) => row.ingredientSkuId).filter((id): id is number => id !== '')
  const rowValid = (row: Row) => row.ingredientSkuId !== '' && Number.isInteger(Number(row.quantity)) && Number(row.quantity) >= 1
  const valid = rows.length > 0 && rows.every(rowValid) && new Set(chosen).size === chosen.length

  const update = (key: number, patch: Partial<Row>) => setRows((list) => list.map((row) => (row.key === key ? { ...row, ...patch } : row)))
  const addRow = () => {
    setRows((list) => [...list, { key: nextKey, ingredientSkuId: '', quantity: '' }])
    setNextKey((k) => k + 1)
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!valid) return
    setBusy(true)
    setError(null)
    try {
      const saved = await hqApi.saveRecipe(
        brand.brandId,
        menu.skuId,
        rows.map((row) => ({ ingredientSkuId: Number(row.ingredientSkuId), quantity: Number(row.quantity) })),
      )
      toast.show(`${menu.displayName} 레시피를 저장했습니다.`, 'success')
      onSaved(saved)
    } catch (caught) {
      setError(hqErrorMessage(caught, '레시피를 저장하지 못했습니다.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="form" onSubmit={submit}>
      <ul className="recipe-rows">
        {rows.map((row, index) => {
          const unit = ingredients.find((sku) => sku.skuId === row.ingredientSkuId)?.unit.description ?? ''
          return (
            <li key={row.key} className="recipe-row">
              <label className="visually-hidden" htmlFor={`ingredient-${row.key}`}>
                식자재 {index + 1}
              </label>
              <select
                id={`ingredient-${row.key}`}
                value={row.ingredientSkuId}
                onChange={(event) => update(row.key, { ingredientSkuId: event.target.value === '' ? '' : Number(event.target.value) })}
              >
                <option value="">식자재 고르기</option>
                {ingredients.map((sku) => (
                  <option key={sku.skuId} value={sku.skuId} disabled={sku.skuId !== row.ingredientSkuId && chosen.includes(sku.skuId)}>
                    {sku.displayName} ({sku.unit.description})
                  </option>
                ))}
              </select>
              <span className="qty">
                <label className="visually-hidden" htmlFor={`qty-${row.key}`}>
                  양 {index + 1}
                </label>
                <input
                  id={`qty-${row.key}`}
                  type="number"
                  inputMode="numeric"
                  min={1}
                  step={1}
                  placeholder="양"
                  value={row.quantity}
                  onChange={(event) => update(row.key, { quantity: event.target.value })}
                />
                <span className="muted small">{unit}</span>
              </span>
              <button
                type="button"
                className="icon-button"
                aria-label={`식자재 ${index + 1} 빼기`}
                disabled={rows.length === 1}
                onClick={() => setRows((list) => list.filter((r) => r.key !== row.key))}
              >
                <IconTrash />
              </button>
            </li>
          )
        })}
      </ul>
      <div>
        <button type="button" className="button ghost small" onClick={addRow} disabled={rows.length >= ingredients.length}>
          <IconPlus className="button-icon" />
          식자재 추가
        </button>
      </div>
      <p className="muted small">메뉴가 결제되면 이 양 × 판매 수량만큼 식자재 재고가 빠집니다.</p>
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
          {busy ? '저장 중…' : '저장'}
        </button>
      </div>
    </form>
  )
}
