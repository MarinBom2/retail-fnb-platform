import { useCallback } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { brandsPath, loginPath, storePath } from '../../routes'
import { storeApi } from '../../shared/api/store/storeService'
import type { MyStore } from '../../shared/api/store/storeService'
import { useAuth } from '../../shared/auth/useAuth'
import { useResource } from '../../shared/hooks/useResource'
import { Badge } from '../../shared/ui/Badge'
import { businessTypeTone } from '../../shared/ui/badgeTone'
import { EmptyState, LoadState } from '../../shared/ui/EmptyState'
import { IconBuilding, IconChevronRight } from '../../shared/ui/icons'
import { readLastStoreId, writeLastStoreId } from '../../shared/utils/storage'
import './stores.css'

/** SCR-03 매장 선택. 브랜드별로 묶어 보여 주고, 마지막에 고른 매장을 기억한다 */
export default function StoreSelectPage() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'checking') return <div className="page-loading" aria-busy="true" />
  if (status === 'anonymous') return <Navigate to={loginPath(location.pathname)} replace />
  return <StoreList />
}

function StoreList() {
  const { member, logout } = useAuth()
  const navigate = useNavigate()
  const loader = useCallback(() => storeApi.myStores(), [])
  const { data, loading, error, reload } = useResource(loader)

  if (!data) {
    return (
      <main className="page">
        <LoadState loading={loading} error={error} onRetry={reload} />
      </main>
    )
  }

  // 매장이 하나뿐이면 바로 들어간다. 본사 화면은 매장 화면 머리글에서 들어갈 수 있다
  if (data.length === 1) {
    return <Navigate to={storePath(data[0].storeId)} replace />
  }

  const last = readLastStoreId()
  const brands = groupByBrand(data)

  return (
    <main className="page store-select">
      <header className="page-header">
        <div>
          <h1>매장 선택</h1>
          <p className="muted">{member?.email} 계정으로 일하는 매장입니다.</p>
        </div>
        <div className="header-actions">
          {data.length > 0 && (
            <Link className="button secondary small" to={brandsPath()}>
              <IconBuilding className="button-icon" />
              본사 관리
            </Link>
          )}
          <button type="button" className="button secondary small" onClick={() => void logout()}>
            로그아웃
          </button>
        </div>
      </header>

      {data.length === 0 && (
        <EmptyState
          title="아직 일하는 매장이 없습니다."
          action={
            <div className="header-actions">
              <Link className="button" to={brandsPath(true)}>
                브랜드 만들기
              </Link>
              <Link className="button secondary" to={brandsPath()}>
                내 브랜드 보기
              </Link>
            </div>
          }
        >
          본사라면 브랜드를 만들고 매장과 상품을 등록하세요. 매장 직원이라면 본사 관리자에게 배정을 요청해 주세요.
        </EmptyState>
      )}

      {brands.map(([brandName, stores]) => (
        <section key={brandName} className="brand-group">
          <h2>
            {brandName} <Badge tone={businessTypeTone(stores[0].businessType.code)}>{stores[0].businessType.description}</Badge>
          </h2>
          <div className="store-grid">
            {stores.map((store) => (
              <Link
                key={store.storeId}
                to={storePath(store.storeId)}
                className={`store-card ${store.storeId === last ? 'is-last' : ''}`}
                onClick={() => writeLastStoreId(store.storeId)}
              >
                <div className="store-card-title">
                  <strong>
                    {store.storeName}
                    {store.storeId === last && <span className="muted small"> · 최근</span>}
                  </strong>
                  <IconChevronRight className="chevron" />
                </div>
                <div className="store-card-meta">
                  <Badge tone={store.status.code === 'OPEN' ? 'success' : 'neutral'}>{store.status.description}</Badge>
                  <Badge>{store.role.description}</Badge>
                  <span className="muted small">{store.storeCode}</span>
                </div>
              </Link>
            ))}
          </div>
        </section>
      ))}
      {data.length > 0 && (
        <p className="muted small store-footnote">
          매장을 고르면 대시보드로 이동합니다.{' '}
          <button type="button" className="link" onClick={() => last && navigate(storePath(last))} disabled={!last}>
            최근 매장으로 바로 가기
          </button>
        </p>
      )}
    </main>
  )
}

function groupByBrand(stores: MyStore[]): [string, MyStore[]][] {
  const map = new Map<string, MyStore[]>()
  for (const store of stores) {
    map.set(store.brandName, [...(map.get(store.brandName) ?? []), store])
  }
  return [...map.entries()]
}
