import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { authApi } from '../api/auth/authService'
import type { MemberLoginRequest } from '../api/auth/authService'
import { memberApi } from '../api/member/memberService'
import type { MemberProfile } from '../api/member/memberService'
import { setUnauthorizedHandler } from '../core/apiClient'
import { AuthContext } from './useAuth'
import type { AuthStatus } from './useAuth'

export function AuthProvider({ children }: { children: ReactNode }) {
  // 토큰은 HttpOnly 쿠키라 서버에 물어본 뒤 정한다
  const [status, setStatus] = useState<AuthStatus>('checking')
  const [member, setMember] = useState<MemberProfile | null>(null)
  const [expiredReason, setExpiredReason] = useState<string | null>(null)

  /** 401 은 어디서 나든 여기로 모인다 */
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setMember(null)
      setStatus('anonymous')
      setExpiredReason('세션이 만료되었습니다. 다시 로그인해주세요.')
    })

    return () => setUnauthorizedHandler(null)
  }, [])

  /** 첫 진입 · 새로고침 때 세션 확인 */
  useEffect(() => {
    let cancelled = false

    memberApi
      .me({ auth: false })
      .then((profile) => {
        if (cancelled) return
        setMember(profile)
        setStatus('authenticated')
      })
      .catch(() => {
        if (cancelled) return
        setMember(null)
        setStatus('anonymous')
      })

    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(async (request: MemberLoginRequest) => {
    // 로그인 응답 본문이 곧 내 정보다. 인증 쿠키는 브라우저가 알아서 저장한다
    const profile = await authApi.login(request)
    setMember(profile)
    setStatus('authenticated')
    setExpiredReason(null)
  }, [])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      // 만료된 토큰이면 실패하지만 화면 정리는 그대로
    } finally {
      setMember(null)
      setStatus('anonymous')
      setExpiredReason(null)
    }
  }, [])

  const value = useMemo(
    () => ({ status, member, expiredReason, login, logout }),
    [status, member, expiredReason, login, logout],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}
