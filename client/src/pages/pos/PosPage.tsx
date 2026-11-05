import { useCallback } from 'react'
import { catalogApi } from '../../shared/api/catalog/catalogService'
import { useResource } from '../../shared/hooks/useResource'
import { isFoodService, useCurrentStore } from '../../shared/store/storeContext'
import { EmptyState, LoadState } from '../../shared/ui/EmptyState'
import { FoodServicePos } from './FoodServicePos'
import { RetailPos } from './RetailPos'
import './pos.css'

/** SCR-05 판매(POS). 업종에 따라 계산대(생활용품·의류) 또는 테이블·포장(외식) 화면 */
export default function PosPage() {
  const { store } = useCurrentStore()
  const loader = useCallback(() => catalogApi.list(store.storeId), [store.storeId])
  const catalog = useResource(loader)

  if (store.status.code !== 'OPEN') {
    return (
      <div className="page-content">
        <EmptyState title={`매장이 ${store.status.description} 상태입니다.`}>영업 중인 매장에서만 주문을 받을 수 있습니다.</EmptyState>
      </div>
    )
  }

  if (!catalog.data) {
    return (
      <div className="page-content">
        <LoadState loading={catalog.loading} error={catalog.error} onRetry={catalog.reload} />
      </div>
    )
  }

  return isFoodService(store) ? (
    <FoodServicePos catalog={catalog.data} onCatalogChanged={catalog.reload} />
  ) : (
    <RetailPos catalog={catalog.data} onCatalogChanged={catalog.reload} />
  )
}
