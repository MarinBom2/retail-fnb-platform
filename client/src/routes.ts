/** 화면 경로. 키는 화면 ID(SCR-xx)를 그대로 써서 산출물 추출이 가능하게 한다 (md/develop-principal.md §3) */
export const ROUTES = {
  SCR01_HOME: '/',
  /** SCR-02 로그인·가입 (?next=, ?mode=signup) */
  SCR02_LOGIN: '/login',
} as const

export const loginPath = (next: string) => `/login?next=${encodeURIComponent(next)}`
