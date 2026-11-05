import { createContext, use } from 'react'
import type { MemberLoginRequest } from '../api/auth/authService'
import type { MemberProfile } from '../api/member/memberService'

export type AuthStatus = 'checking' | 'authenticated' | 'anonymous'

export type AuthContextValue = {
  status: AuthStatus
  member: MemberProfile | null
  /** 세션이 끊겨 튕긴 경우에만 채워진다 */
  expiredReason: string | null
  login: (request: MemberLoginRequest) => Promise<void>
  logout: () => Promise<void>
}

/** AuthProvider 와 따로 둔다 (Fast Refresh) */
export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const context = use(AuthContext)
  if (!context) {
    throw new Error('useAuth 는 AuthProvider 안에서만 쓸 수 있습니다.')
  }
  return context
}
