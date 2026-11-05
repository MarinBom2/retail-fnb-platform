/** 브라우저 저장소는 막혀 있을 수 있다 (사생활 보호 모드). 실패해도 화면은 동작해야 한다 */
const LAST_STORE_KEY = 'retailfnb.lastStoreId'

export function readLastStoreId(): number | null {
  try {
    const value = Number(window.localStorage.getItem(LAST_STORE_KEY))
    return Number.isInteger(value) && value > 0 ? value : null
  } catch {
    return null
  }
}

export function writeLastStoreId(storeId: number) {
  try {
    window.localStorage.setItem(LAST_STORE_KEY, String(storeId))
  } catch {
    // 저장하지 못해도 다음에 매장을 다시 고르면 된다
  }
}
