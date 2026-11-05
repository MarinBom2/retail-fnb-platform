import { useCallback } from 'react'
import { Link } from 'react-router-dom'
import { useStockActions } from '../../features/stock/stockActionsContext'
import { storePath } from '../../routes'
import { dashboardApi } from '../../shared/api/dashboard/dashboardService'
import type { Dashboard, LowStockItem } from '../../shared/api/dashboard/dashboardService'
import { useResource } from '../../shared/hooks/useResource'
import { isFoodService, useCurrentStore } from '../../shared/store/storeContext'
import { EmptyState, LoadState } from '../../shared/ui/EmptyState'
import { IconChevronRight, IconRefresh } from '../../shared/ui/icons'
import { formatBusinessDate, formatNumber, formatQuantity, formatWon } from '../../shared/utils/format'
import './dashboard.css'

/** SCR-04 매장 대시보드 (FR-DA-001). 오늘 매출, 시간대별 매출, 부족 재고와 추천 매장, 처리할 이동 */
export default function DashboardPage() {
  const { store } = useCurrentStore()
  const loader = useCallback(() => dashboardApi.get(store.storeId), [store.storeId])
  const { data, loading, error, reload } = useResource(loader)

  return (
    <div className="page-content">
      <header className="page-header">
        <div>
          <h1>대시보드</h1>
          <p className="muted">
            {store.brandName} {store.storeName}
            {data ? ` · ${formatBusinessDate(data.businessDate)} 영업일` : ''}
          </p>
        </div>
        <div className="header-actions">
          <button type="button" className="button secondary small" onClick={reload} disabled={loading}>
            <IconRefresh className="button-icon" />
            새로고침
          </button>
          <Link className="button small" to={storePath(store.storeId, 'pos')}>
            {isFoodService(store) ? '주문 받기' : '판매하기'}
          </Link>
        </div>
      </header>

      {!data && <LoadState loading={loading} error={error} onRetry={reload} />}
      {data && <DashboardBody data={data} />}
    </div>
  )
}

function DashboardBody({ data }: { data: Dashboard }) {
  const { store } = useCurrentStore()
  const food = isFoodService(store)

  return (
    <>
      <section className="kpi-grid" aria-label="오늘 지표">
        <Kpi label="오늘 매출" value={formatWon(data.salesAmount)} />
        <Kpi label="결제 건수" value={`${formatNumber(data.orderCount)}건`} />
        <Kpi label="객단가" value={formatWon(data.averageTicket)} />
        <Kpi label="환불" value={formatWon(data.refundedAmount)} tone={data.refundedAmount > 0 ? 'warning' : undefined} />
      </section>

      <div className="dashboard-grid">
        <section className="card span-2">
          <div className="card-header">
            <h2>시간대별 매출</h2>
            <span className="muted small">한국 시간</span>
          </div>
          <HourlyChart data={data.hourlySales} />
        </section>

        <section className="card">
          <div className="card-header">
            <h2>할 일</h2>
          </div>
          <ul className="task-list">
            <TaskRow
              to={storePath(store.storeId, food ? 'pos' : 'orders')}
              label={food ? '이용 중인 테이블·주문' : '결제 전 주문'}
              count={data.openOrderCount}
            />
            <TaskRow to={storePath(store.storeId, 'transfers')} label="승인 기다리는 이동 요청" count={data.transfersToApprove} />
            <TaskRow to={storePath(store.storeId, 'transfers')} label="출고할 이동" count={data.transfersToShip} />
            <TaskRow to={storePath(store.storeId, 'transfers')} label="입고 확인할 이동" count={data.transfersToReceive} />
          </ul>
        </section>

        <section className="card">
          <div className="card-header">
            <h2>많이 팔린 {food ? '메뉴' : '상품'}</h2>
            <span className="muted small">오늘</span>
          </div>
          {data.topItems.length === 0 ? (
            <EmptyState title="오늘 판매가 아직 없습니다." />
          ) : (
            <ol className="rank-list">
              {data.topItems.map((item, index) => (
                <li key={item.skuId}>
                  <span className="rank">{index + 1}</span>
                  <span className="grow">{item.itemName}</span>
                  <span className="muted small rank-qty">{formatNumber(item.quantity)}개</span>
                  <strong className="rank-amount">{formatWon(item.amount)}</strong>
                </li>
              ))}
            </ol>
          )}
        </section>

        <section className="card span-2">
          <div className="card-header">
            <h2>안전재고 이하 {food ? '식자재' : '상품'}</h2>
            <Link className="link small" to={storePath(store.storeId, 'inventory')}>
              재고 전체 보기
            </Link>
          </div>
          {data.lowStock.length === 0 ? (
            <EmptyState title="안전재고보다 적은 품목이 없습니다.">재고 화면에서 품목별 안전재고를 정할 수 있습니다.</EmptyState>
          ) : (
            <ul className="low-stock-list">
              {data.lowStock.map((item) => (
                <LowStockRow key={item.skuId} item={item} />
              ))}
            </ul>
          )}
        </section>
      </div>
    </>
  )
}

function Kpi({ label, value, tone }: { label: string; value: string; tone?: 'warning' }) {
  return (
    <div className={`kpi ${tone ? `kpi-${tone}` : ''}`}>
      <span className="kpi-label">{label}</span>
      <strong className="kpi-value">{value}</strong>
    </div>
  )
}

function TaskRow({ to, label, count }: { to: string; label: string; count: number }) {
  return (
    <li>
      <Link to={to} className={`task-row ${count > 0 ? 'has-count' : ''}`}>
        <span>{label}</span>
        <span className="task-row-right">
          <span className="task-count">{formatNumber(count)}</span>
          <IconChevronRight className="chevron" />
        </span>
      </Link>
    </li>
  )
}

function LowStockRow({ item }: { item: LowStockItem }) {
  const { store } = useCurrentStore()
  const { openTransferRequest } = useStockActions()
  const ratio = item.reorderPoint > 0 ? Math.min(1, item.quantity / item.reorderPoint) : 0
  const suggested = Math.max(1, item.reorderPoint * 2 - item.quantity)
  const quantity = item.bestSourceQuantity ? Math.min(suggested, item.bestSourceQuantity) : suggested

  return (
    <li className="low-stock-row">
      <div className="grow">
        <strong>{item.itemName}</strong>
        <div className={`meter ${item.quantity === 0 ? 'danger' : ''}`} aria-hidden="true">
          <span style={{ width: `${ratio * 100}%` }} />
        </div>
        <span className="muted small">
          현재 {formatQuantity(item.quantity, item.unit)} / 안전재고 {formatQuantity(item.reorderPoint, item.unit)}
        </span>
      </div>
      <div className="low-stock-source">
        {item.bestSourceStoreId ? (
          <>
            <span className="small">
              추천: <strong>{item.bestSourceStoreName}</strong>
              <span className="muted"> ({formatQuantity(item.bestSourceQuantity ?? 0, item.unit)})</span>
            </span>
            {store.canManageInventory && (
              <button
                type="button"
                className="button small"
                onClick={() =>
                  openTransferRequest({ skuId: item.skuId, fromStoreId: item.bestSourceStoreId ?? undefined, quantity })
                }
              >
                이동 요청
              </button>
            )}
          </>
        ) : (
          <span className="muted small">다른 매장에도 재고가 없습니다</span>
        )}
      </div>
    </li>
  )
}

/** 24칸 막대 그래프. 값은 막대 title 로 읽힌다 (스크린리더는 표 대체 텍스트) */
function HourlyChart({ data }: { data: { hour: number; amount: number }[] }) {
  const max = Math.max(1, ...data.map((point) => point.amount))
  const width = 480
  const height = 160
  const barGap = 4
  const barWidth = width / 24 - barGap
  const total = data.reduce((sum, point) => sum + point.amount, 0)

  if (total === 0) return <EmptyState title="오늘 결제가 아직 없습니다." />

  const busiest = data.reduce((best, point) => (point.amount > best.amount ? point : best), data[0])

  return (
    <figure className="chart">
      <svg viewBox={`0 0 ${width} ${height + 20}`} role="img" aria-label={`시간대별 매출. 가장 많은 시간 ${busiest.hour}시 ${formatWon(busiest.amount)}`}>
        {[0.25, 0.5, 0.75, 1].map((ratio) => (
          <line key={ratio} x1={0} x2={width} y1={height - height * ratio} y2={height - height * ratio} className="chart-grid" />
        ))}
        {data.map((point) => {
          const barHeight = (point.amount / max) * (height - 8)
          const x = point.hour * (width / 24) + barGap / 2
          return (
            <g key={point.hour}>
              <rect
                x={x}
                y={height - barHeight}
                width={barWidth}
                height={Math.max(barHeight, point.amount > 0 ? 2 : 0)}
                rx={Math.min(4, barWidth / 2)}
                className={point.hour === busiest.hour ? 'chart-bar is-peak' : 'chart-bar'}
              >
                <title>{`${point.hour}시 ${formatWon(point.amount)}`}</title>
              </rect>
              {point.hour % 3 === 0 && (
                <text x={x + barWidth / 2} y={height + 15} textAnchor="middle" className="chart-label">
                  {point.hour}
                </text>
              )}
            </g>
          )
        })}
      </svg>
      <figcaption className="muted small">
        가장 바쁜 시간 {busiest.hour}시 · {formatWon(busiest.amount)}
      </figcaption>
    </figure>
  )
}
