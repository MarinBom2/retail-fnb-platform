import { ApiError } from '../core/apiClient'

/** 화면에 보여줄 오류 문장. 서버 문장(ApiError.message)만 그대로, 그 밖은 fallback */
export function errorMessage(caught: unknown, fallback: string): string {
  if (caught instanceof ApiError) {
    return caught.message || fallback
  }
  if (caught instanceof DOMException && caught.name === 'AbortError') {
    return '요청을 중단했습니다.'
  }
  // fetch 는 네트워크 단계에서 TypeError 를 던진다
  if (caught instanceof TypeError) {
    return '서버에 연결하지 못했습니다. 네트워크 상태를 확인해주세요.'
  }
  return fallback
}
