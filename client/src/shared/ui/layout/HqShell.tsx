import { useCallback, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { Link, Navigate, NavLink, Outlet, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ROUTES, brandPath, brandsPath, loginPath } from '../../../routes'
import type { BrandSection } from '../../../routes'
import { hqApi } from '../../api/hq/hqService'
import { useAuth } from '../../auth/useAuth'
import { HqContext } from '../../hq/hqContext'
import { useResource } from '../../hooks/useResource'
import { Badge } from '../Badge'
import { businessTypeTone } from '../badgeTone'
import { Dialog } from '../Dialog'
import { EmptyState, LoadState } from '../EmptyState'
import { IconBag, IconBuilding, IconStore, IconTag } from '../icons'
import { Menu } from '../Menu'
import { useToast } from '../toastContext'

const NAV: { section: BrandSection; label: string; icon: ReactNode }[] = [
  { section: 'stores', label: '매장', icon: <IconStore /> },
  { section: 'products', label: '상품', icon: <IconTag /> },
]

/** SCR-11·12 공통 틀: 브랜드 바꾸기, 본사 메뉴, 매장 운영으로 돌아가기. 브랜드 관리자 권한은 서버가 다시 확인한다 */
export default function HqShell() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'checking') return <div className="page-loading" aria-busy="true" />
  if (status === 'anonymous') return <Navigate to={loginPath(location.pathname + location.search)} replace />
  return <AuthenticatedHqShell />
}

function AuthenticatedHqShell() {
  const { member, logout } = useAuth()
  const params = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const brandId = Number(params.brandId)
  const [renaming, setRenaming] = useState(false)

  const loader = useCallback(() => hqApi.brands(), [])
  const { data: brands, loading, error, reload, replace } = useResource(loader)
  const brand = brands?.find((candidate) => candidate.brandId === brandId)
  const section = (location.pathname.split('/')[3] ?? 'stores') as BrandSection

  if (!brands) {
    return (
      <div className="page">
        <LoadState loading={loading} error={error} onRetry={reload} />
      </div>
    )
  }

  if (!brand) {
    return (
      <div className="page narrow">
        <EmptyState title="이 브랜드를 관리할 수 없습니다.">
          <Link className="link" to={ROUTES.SCR10_BRANDS}>
            브랜드 목록으로
          </Link>
        </EmptyState>
      </div>
    )
  }

  return (
    <HqContext value={{ brand, brands, reloadBrands: reload }}>
      <div className="app-shell hq-shell">
        <header className="app-header">
          <Link className="app-logo" to={ROUTES.SCR10_BRANDS} aria-label="브랜드 목록으로">
            <span className="logo-mark" aria-hidden="true" />
            <span className="logo-text">Retail F&amp;B</span>
            <span className="hq-mark">본사</span>
          </Link>
          <div className="store-switcher">
            {brands.length > 1 ? (
              <>
                <label className="visually-hidden" htmlFor="brand-switcher">
                  브랜드 바꾸기
                </label>
                <select
                  id="brand-switcher"
                  value={brand.brandId}
                  onChange={(event) => navigate(brandPath(Number(event.target.value), section))}
                >
                  {brands.map((candidate) => (
                    <option key={candidate.brandId} value={candidate.brandId}>
                      {candidate.name}
                    </option>
                  ))}
                </select>
              </>
            ) : (
              <strong className="hq-brand-name">{brand.name}</strong>
            )}
            <Badge tone={businessTypeTone(brand.businessType.code)}>{brand.businessType.description}</Badge>
            <Menu label="브랜드 설정" items={[{ label: '브랜드 이름 바꾸기', onSelect: () => setRenaming(true) }]} />
          </div>
          <div className="app-user">
            <span className="muted small user-email">{member?.email} · 본사 관리자</span>
            <Link className="button secondary small hq-ops-link" to={ROUTES.SCR03_STORES}>
              매장 운영
            </Link>
            <button type="button" className="button secondary small" onClick={() => void logout()}>
              로그아웃
            </button>
          </div>
        </header>

        <nav className="app-nav" aria-label="본사 메뉴">
          {NAV.map((item) => (
            <NavLink key={item.section} to={brandPath(brand.brandId, item.section)} aria-label={item.label}>
              <span className="nav-icon" aria-hidden="true">
                {item.icon}
              </span>
              <span className="nav-label-long">{item.label} 관리</span>
              <span className="nav-label-short">{item.label}</span>
            </NavLink>
          ))}
          <NavLink to={brandsPath()} end aria-label="브랜드 목록">
            <span className="nav-icon" aria-hidden="true">
              <IconBuilding />
            </span>
            <span className="nav-label-long">브랜드 목록</span>
            <span className="nav-label-short">브랜드</span>
          </NavLink>
          <NavLink to={ROUTES.SCR03_STORES} end className="hq-nav-ops" aria-label="매장 운영">
            <span className="nav-icon" aria-hidden="true">
              <IconBag />
            </span>
            <span className="nav-label-long">매장 운영으로</span>
            <span className="nav-label-short">운영</span>
          </NavLink>
        </nav>

        <main className="app-main">
          <Outlet />
        </main>
      </div>

      {renaming && (
        <RenameBrandDialog
          brandId={brand.brandId}
          current={brand.name}
          onClose={() => setRenaming(false)}
          onSaved={(updated) => {
            replace(brands.map((candidate) => (candidate.brandId === updated.brandId ? updated : candidate)))
            setRenaming(false)
          }}
        />
      )}
    </HqContext>
  )
}

function RenameBrandDialog({
  brandId,
  current,
  onClose,
  onSaved,
}: {
  brandId: number
  current: string
  onClose: () => void
  onSaved: (brand: Awaited<ReturnType<typeof hqApi.renameBrand>>) => void
}) {
  const toast = useToast()
  const [name, setName] = useState(current)
  const [busy, setBusy] = useState(false)
  const trimmed = name.trim()

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!trimmed) return
    setBusy(true)
    try {
      const updated = await hqApi.renameBrand(brandId, trimmed)
      toast.show('브랜드 이름을 바꿨습니다.', 'success')
      onSaved(updated)
    } catch (caught) {
      toast.error(caught, '이름을 바꾸지 못했습니다.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open title="브랜드 이름 바꾸기" size="sm" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <label>
          브랜드 이름
          <input value={name} maxLength={100} required onChange={(event) => setName(event.target.value)} />
        </label>
        <div className="dialog-actions">
          <button type="button" className="button secondary" onClick={onClose}>
            닫기
          </button>
          <button type="submit" className="button" disabled={!trimmed || trimmed === current || busy}>
            {busy ? '저장 중…' : '저장'}
          </button>
        </div>
      </form>
    </Dialog>
  )
}
