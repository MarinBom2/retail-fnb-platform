import { useCallback, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { ROUTES, brandPath, loginPath } from '../../routes'
import { hqApi } from '../../shared/api/hq/hqService'
import type { BrandSummary, BusinessTypeCode } from '../../shared/api/hq/hqService'
import { useAuth } from '../../shared/auth/useAuth'
import { useResource } from '../../shared/hooks/useResource'
import { Badge } from '../../shared/ui/Badge'
import { businessTypeTone } from '../../shared/ui/badgeTone'
import { Dialog } from '../../shared/ui/Dialog'
import { LoadState } from '../../shared/ui/EmptyState'
import { IconChevronRight, IconPlus } from '../../shared/ui/icons'
import { useToast } from '../../shared/ui/toastContext'
import { formatNumber } from '../../shared/utils/format'
import { hqErrorMessage } from './hqUtils'
import './hq.css'

/** 업종을 고를 때 무엇이 달라지는지 한 줄로 보여 준다 */
const BUSINESS_TYPES: { code: BusinessTypeCode; label: string; dot: string; detail: string }[] = [
  { code: 'GENERAL_MERCHANDISE', label: '생활용품 리테일', dot: 'gm', detail: '상품마다 바코드 SKU 하나. 계산대에서 스캔해 팝니다.' },
  { code: 'APPAREL', label: '의류 리테일', dot: 'apparel', detail: '스타일마다 색상 × 사이즈 SKU. 매장 간 사이즈 이동이 잦습니다.' },
  { code: 'FOOD_SERVICE', label: '외식', dot: 'food', detail: '메뉴와 식자재를 나누고, 레시피대로 식자재 재고가 빠집니다.' },
]

/** SCR-10 본사: 내가 관리하는 브랜드 목록과 브랜드 만들기 (FR-ST-001) */
export default function BrandListPage() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'checking') return <div className="page-loading" aria-busy="true" />
  if (status === 'anonymous') return <Navigate to={loginPath(location.pathname + location.search)} replace />
  return <BrandList />
}

function BrandList() {
  const { member, logout } = useAuth()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const loader = useCallback(() => hqApi.brands(), [])
  const { data, loading, error, reload } = useResource(loader)
  const creating = params.get('create') === '1'

  const setCreating = (open: boolean) => setParams(open ? { create: '1' } : {}, { replace: true })

  return (
    <main className="page brand-list">
      <header className="page-header">
        <div>
          <h1>본사 관리</h1>
          <p className="muted">{member?.email} 계정이 관리하는 브랜드입니다.</p>
        </div>
        <div className="header-actions">
          <Link className="button secondary small" to={ROUTES.SCR03_STORES}>
            매장 운영
          </Link>
          <button type="button" className="button secondary small" onClick={() => void logout()}>
            로그아웃
          </button>
          {data && data.length > 0 && (
            <button type="button" className="button small" onClick={() => setCreating(true)}>
              <IconPlus className="button-icon" />
              브랜드 만들기
            </button>
          )}
        </div>
      </header>

      {!data && <LoadState loading={loading} error={error} onRetry={reload} />}

      {data && data.length === 0 && <Onboarding onStart={() => setCreating(true)} />}

      {data && data.length > 0 && (
        <div className="brand-grid">
          {data.map((brand) => (
            <BrandCard key={brand.brandId} brand={brand} />
          ))}
        </div>
      )}

      {creating && (
        <CreateBrandDialog
          onClose={() => setCreating(false)}
          onCreated={(brand) => navigate(brandPath(brand.brandId, 'stores'))}
        />
      )}
    </main>
  )
}

function BrandCard({ brand }: { brand: BrandSummary }) {
  return (
    <Link className="brand-card" to={brandPath(brand.brandId)}>
      <div className="brand-card-title">
        <strong>{brand.name}</strong>
        <IconChevronRight className="chevron" />
      </div>
      <div>
        <Badge tone={businessTypeTone(brand.businessType.code)}>{brand.businessType.description}</Badge>
      </div>
      <dl className="brand-stats">
        <div>
          <dt>매장</dt>
          <dd>
            {formatNumber(brand.storeCount)}곳
            <span className="muted small"> · 영업 중 {formatNumber(brand.openStoreCount)}</span>
          </dd>
        </div>
        <div>
          <dt>상품</dt>
          <dd>{formatNumber(brand.productCount)}개</dd>
        </div>
      </dl>
    </Link>
  )
}

/** 처음 온 사람에게 세 단계 온보딩을 보여 준다 */
function Onboarding({ onStart }: { onStart: () => void }) {
  return (
    <section className="onboarding" aria-labelledby="onboarding-title">
      <h2 id="onboarding-title">세 단계면 매장에서 바로 팔 수 있습니다</h2>
      <ol className="onboarding-steps">
        <li>
          <span className="step-number">1</span>
          <strong>브랜드 만들기</strong>
          <span className="muted small">이름과 업종을 정합니다. 업종은 나중에 바꿀 수 없습니다.</span>
        </li>
        <li>
          <span className="step-number">2</span>
          <strong>매장 추가</strong>
          <span className="muted small">매장 코드와 이름을 넣고 영업을 시작합니다. 직원을 배정합니다.</span>
        </li>
        <li>
          <span className="step-number">3</span>
          <strong>상품 등록</strong>
          <span className="muted small">업종에 맞는 방식으로 SKU 를 만들면 모든 매장 POS 에 나옵니다.</span>
        </li>
      </ol>
      <button type="button" className="button large" onClick={onStart}>
        <IconPlus className="button-icon" />
        브랜드 만들기
      </button>
    </section>
  )
}

function CreateBrandDialog({ onClose, onCreated }: { onClose: () => void; onCreated: (brand: BrandSummary) => void }) {
  const toast = useToast()
  const [name, setName] = useState('')
  const [type, setType] = useState<BusinessTypeCode | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const trimmed = name.trim()
  const valid = trimmed.length > 0 && type !== null

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!valid || !type) return
    setBusy(true)
    setError(null)
    try {
      const brand = await hqApi.createBrand(trimmed, type)
      toast.show(`${brand.name} 브랜드를 만들었습니다. 매장을 추가해 주세요.`, 'success')
      onCreated(brand)
    } catch (caught) {
      setError(hqErrorMessage(caught, '브랜드를 만들지 못했습니다.', { ALREADY_EXIST: '이미 있는 브랜드 이름입니다.' }))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open title="브랜드 만들기" description="만든 사람이 이 브랜드의 본사 관리자가 됩니다." onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <label>
          브랜드 이름
          <input value={name} maxLength={100} required placeholder="예: 우리동네마트" onChange={(event) => setName(event.target.value)} />
        </label>
        <fieldset className="type-choices">
          <legend>업종 (나중에 바꿀 수 없습니다)</legend>
          {BUSINESS_TYPES.map((option) => (
            <label key={option.code} className={`type-choice ${type === option.code ? 'selected' : ''}`}>
              <input
                type="radio"
                name="business-type"
                value={option.code}
                checked={type === option.code}
                onChange={() => setType(option.code)}
              />
              <span className={`type-dot ${option.dot}`} aria-hidden="true" />
              <strong>{option.label}</strong>
              <span className="muted small">{option.detail}</span>
            </label>
          ))}
        </fieldset>
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
            {busy ? '만드는 중…' : '만들기'}
          </button>
        </div>
      </form>
    </Dialog>
  )
}
