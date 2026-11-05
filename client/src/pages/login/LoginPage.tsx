import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { ROUTES } from '../../routes'
import { authApi } from '../../shared/api/auth/authService'
import { useAuth } from '../../shared/auth/useAuth'
import { errorMessage } from '../../shared/utils/errorMessage'
import './login.css'

/** 데모 계정 (md/api.md §7). demo 프로필 서버에서만 있다 */
const DEMO_EMAIL = 'demo@retailfnb.dev'
const DEMO_PASSWORD = 'Demo1234!'

/** SCR-02 로그인·가입 (FR-AU-001, FR-AU-002). ?mode=signup 이면 가입, ?demo=1 이면 데모 계정을 채운다 */
export default function LoginPage() {
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()
  const { login, expiredReason } = useAuth()
  const signupMode = params.get('mode') === 'signup'
  const demoMode = params.get('demo') === '1'

  const [email, setEmail] = useState(demoMode ? DEMO_EMAIL : '')
  const [password, setPassword] = useState(demoMode ? DEMO_PASSWORD : '')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      if (signupMode) {
        await authApi.signup({ email, password })
      }
      await login({ email, password })
      navigate(params.get('next') ?? ROUTES.SCR03_STORES, { replace: true })
    } catch (caught) {
      setError(errorMessage(caught, signupMode ? '가입하지 못했습니다.' : '로그인하지 못했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-card">
        <Link to={ROUTES.SCR01_LANDING} className="app-logo">
          <span className="logo-mark" aria-hidden="true" />
          <span className="logo-text">Retail F&B</span>
        </Link>
        <h1>{signupMode ? '가입' : '로그인'}</h1>
        {expiredReason && !signupMode && <p className="notice-box">{expiredReason}</p>}
        {demoMode && !signupMode && (
          <p className="notice-box">데모 계정이 채워져 있습니다. 로그인하면 가상 브랜드 매장 세 곳을 둘러볼 수 있습니다.</p>
        )}

        <form className="form" onSubmit={handleSubmit}>
          <label>
            이메일
            <input type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </label>
          <label>
            비밀번호
            <input
              type="password"
              autoComplete={signupMode ? 'new-password' : 'current-password'}
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>
          {signupMode && <p className="muted small">비밀번호는 6~20자, 영문 대소문자·숫자·특수문자 중 두 가지 이상</p>}
          {error && (
            <p className="error" role="alert">
              {error}
            </p>
          )}
          <button className="button large" type="submit" disabled={submitting}>
            {submitting ? '처리 중…' : signupMode ? '가입하고 로그인' : '로그인'}
          </button>
        </form>

        <div className="auth-links">
          <button
            type="button"
            className="link"
            onClick={() => setParams(signupMode ? {} : { mode: 'signup' }, { replace: true })}
          >
            {signupMode ? '이미 계정이 있어요' : '처음이에요 (가입)'}
          </button>
          {!demoMode && (
            <Link className="link" to="/login?demo=1" reloadDocument>
              데모 계정으로
            </Link>
          )}
        </div>
      </div>
    </main>
  )
}
