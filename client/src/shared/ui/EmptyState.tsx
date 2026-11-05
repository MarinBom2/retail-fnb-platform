import type { ReactNode } from 'react'

export function EmptyState({ title, children, action }: { title: string; children?: ReactNode; action?: ReactNode }) {
  return (
    <div className="empty-state">
      <p className="empty-title">{title}</p>
      {children && <div className="muted small">{children}</div>}
      {action}
    </div>
  )
}

/** 조회 상태 공통 표시. 데이터가 있으면 children */
export function LoadState({ loading, error, onRetry }: { loading: boolean; error: unknown; onRetry: () => void }) {
  if (error) {
    return (
      <div className="empty-state" role="alert">
        <p className="empty-title">불러오지 못했습니다.</p>
        <button type="button" className="button secondary" onClick={onRetry}>
          다시 시도
        </button>
      </div>
    )
  }
  if (loading) {
    return <div className="skeleton" aria-busy="true" aria-label="불러오는 중" />
  }
  return null
}
