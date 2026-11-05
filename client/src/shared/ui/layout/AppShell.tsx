import { useCallback, useEffect } from 'react'
import { Link, Navigate, NavLink, Outlet, useLocation, useNavigate, useParams } from 'react-router-dom'
import { StockActionsProvider } from '../../../features/stock/StockActionsProvider'
import { ROUTES, loginPath, storePath } from '../../../routes'
import type { StoreSection } from '../../../routes'
import { storeApi } from '../../api/store/storeService'
import { useAuth } from '../../auth/useAuth'
import { useResource } from '../../hooks/useResource'
import { StoreContext, isFoodService } from '../../store/storeContext'
import { writeLastStoreId } from '../../utils/storage'
import { Badge } from '../Badge'
import { businessTypeTone } from '../badgeTone'
import { EmptyState, LoadState } from '../EmptyState'

const NAV: { section: StoreSection; label: string; icon: string }[] = [
  { section: '', label: '대시보드', icon: '▦' },
  { section: 'pos', label: '판매(POS)', icon: '₩' },
  { section: 'orders', label: '주문 내역', icon: '≡' },
  { section: 'inventory', label: '재고', icon: '▤' },
  { section: 'transfers', label: '매장 간 이동', icon: '⇄' },
  { section: 'catalog', label: '상품', icon: '◇' },
]

/** SCR-04~09 공통 틀: 매장 바꾸기, 메뉴, 로그아웃. 매장 권한은 서버가 다시 확인한다 */
export default function AppShell() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'checking') return <div className="page-loading" aria-busy="true" />
  if (status === 'anonymous') return <Navigate to={loginPath(location.pathname + location.search)} replace />
  return <AuthenticatedShell />
}

function AuthenticatedShell() {
  const { member, logout } = useAuth()
  const params = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const storeId = Number(params.storeId)

  const loader = useCallback(() => storeApi.myStores(), [])
  const { data: stores, loading, error, reload } = useResource(loader)
  const store = stores?.find((candidate) => candidate.storeId === storeId)

  const section = (location.pathname.split('/')[3] ?? '') as StoreSection
  const currentStoreId = store?.storeId

  // 다음에 들어올 때 이 매장으로 바로 연다
  useEffect(() => {
    if (currentStoreId) writeLastStoreId(currentStoreId)
  }, [currentStoreId])

  if (!stores) {
    return (
      <div className="page">
        <LoadState loading={loading} error={error} onRetry={reload} />
      </div>
    )
  }

  if (!store) {
    return (
      <div className="page narrow">
        <EmptyState title="이 매장에 접근할 수 없습니다.">
          <Link className="link" to={ROUTES.SCR03_STORES}>
            매장 다시 고르기
          </Link>
        </EmptyState>
      </div>
    )
  }

  return (
    <StoreContext value={{ store, stores }}>
      <StockActionsProvider key={store.storeId}>
        <div className="app-shell">
          <header className="app-header">
            <Link className="app-logo" to={ROUTES.SCR03_STORES} aria-label="매장 선택으로">
              <span className="logo-mark" aria-hidden="true" />
              <span className="logo-text">Retail F&B</span>
            </Link>
            <div className="store-switcher">
              <label className="visually-hidden" htmlFor="store-switcher">
                매장 바꾸기
              </label>
              <select
                id="store-switcher"
                value={store.storeId}
                onChange={(event) => {
                  const next = Number(event.target.value)
                  writeLastStoreId(next)
                  navigate(storePath(next, section))
                }}
              >
                {stores.map((candidate) => (
                  <option key={candidate.storeId} value={candidate.storeId}>
                    {candidate.brandName} · {candidate.storeName}
                  </option>
                ))}
              </select>
              <Badge tone={businessTypeTone(store.businessType.code)}>{store.businessType.description}</Badge>
              {store.status.code !== 'OPEN' && <Badge tone="danger">{store.status.description}</Badge>}
            </div>
            <div className="app-user">
              <span className="muted small user-email">
                {member?.email} · {store.role.description}
              </span>
              <button type="button" className="button secondary small" onClick={() => void logout()}>
                로그아웃
              </button>
            </div>
          </header>

          <nav className="app-nav" aria-label="매장 메뉴">
            {NAV.map((item) => (
              <NavLink key={item.section} to={storePath(store.storeId, item.section)} end={item.section === ''}>
                <span className="nav-icon" aria-hidden="true">
                  {item.icon}
                </span>
                <span>{item.section === 'pos' && isFoodService(store) ? '주문·결제' : item.label}</span>
              </NavLink>
            ))}
          </nav>

          <main className="app-main">
            <Outlet />
          </main>
        </div>
      </StockActionsProvider>
    </StoreContext>
  )
}
