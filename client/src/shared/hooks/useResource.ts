import { useEffect, useState } from 'react'

type ResourceState<T> = {
  loader: (() => Promise<T>) | null
  version: number
  data?: T
  error?: unknown
}

/**
 * 서버 조회 하나. loader 는 useCallback 으로 고정해서 넘긴다 (바뀌면 다시 읽는다).
 * reload 는 기존 데이터를 둔 채로 다시 읽어 화면이 깜빡이지 않는다.
 */
export function useResource<T>(loader: () => Promise<T>) {
  const [version, setVersion] = useState(0)
  const [state, setState] = useState<ResourceState<T>>({ loader: null, version: -1 })

  useEffect(() => {
    let cancelled = false
    loader().then(
      (data) => {
        if (!cancelled) setState({ loader, version, data, error: undefined })
      },
      (error: unknown) => {
        if (!cancelled) setState((prev) => ({ loader, version, data: prev.data, error }))
      },
    )
    return () => {
      cancelled = true
    }
  }, [loader, version])

  const loading = state.loader !== loader || state.version !== version
  // 다른 대상을 읽는 중이면 이전 대상의 데이터를 보여 주지 않는다
  const sameTarget = state.loader === loader

  return {
    data: sameTarget ? state.data : undefined,
    error: sameTarget && !loading ? state.error : undefined,
    loading,
    reload: () => setVersion((v) => v + 1),
    /** 서버 응답으로 바로 바꿀 때 (다시 읽지 않고) */
    replace: (data: T) => setState((prev) => ({ ...prev, data })),
  }
}
