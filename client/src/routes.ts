/** 화면 경로. 키는 화면 ID(SCR-xx)를 그대로 써서 산출물 추출이 가능하게 한다 (md/develop-principal.md §3) */
export const ROUTES = {
  /** SCR-01 소개(비로그인). 로그인 상태면 매장으로 보낸다 */
  SCR01_LANDING: '/',
  /** SCR-02 로그인·가입 (?next=, ?mode=signup, ?demo=1) */
  SCR02_LOGIN: '/login',
  /** SCR-03 매장 선택 */
  SCR03_STORES: '/stores',
  /** SCR-04~09 는 매장 화면 (AppShell 아래) */
  SCR04_DASHBOARD: '/stores/:storeId',
  SCR05_POS: '/stores/:storeId/pos',
  SCR06_ORDERS: '/stores/:storeId/orders',
  SCR07_INVENTORY: '/stores/:storeId/inventory',
  SCR08_TRANSFERS: '/stores/:storeId/transfers',
  SCR09_CATALOG: '/stores/:storeId/catalog',
  /** SCR-10 본사: 내 브랜드 목록·브랜드 만들기 (?create=1 이면 만들기 창을 바로 연다) */
  SCR10_BRANDS: '/brands',
  /** SCR-11·12 는 본사 화면 (HqShell 아래) */
  SCR11_BRAND_STORES: '/brands/:brandId/stores',
  SCR12_BRAND_PRODUCTS: '/brands/:brandId/products',
} as const

/** 매장 화면의 하위 경로. 매장을 바꿀 때 같은 화면을 유지하는 데도 쓴다 */
export type StoreSection = '' | 'pos' | 'orders' | 'inventory' | 'transfers' | 'catalog'

export const storePath = (storeId: number, section: StoreSection = '') =>
  section ? `/stores/${storeId}/${section}` : `/stores/${storeId}`

export const loginPath = (next?: string) => (next ? `/login?next=${encodeURIComponent(next)}` : '/login')

export const demoLoginPath = () => '/login?demo=1'

/** 본사 화면의 하위 경로 */
export type BrandSection = 'stores' | 'products'

export const brandPath = (brandId: number, section: BrandSection = 'stores') => `/brands/${brandId}/${section}`

export const brandsPath = (create = false) => (create ? '/brands?create=1' : '/brands')
