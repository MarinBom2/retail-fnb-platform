import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { ROUTES } from '../../routes'
import { authApi } from '../../shared/api/auth/authService'
import { useAuth } from '../../shared/auth/useAuth'
import { errorMessage } from '../../shared/utils/errorMessage'

/** SCR-02 로그인·가입 (FR-AU-001, FR-AU-002). ?mode=signup 이면 가입 폼 */
export default function LoginPage() {
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()
  const { login, expiredReason } = useAuth()
  const signupMode = params.get('mode') === 'signup'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
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
      navigate(params.get('next') ?? ROUTES.SCR01_HOME, { replace: true })
    } catch (caught) {
      setError(errorMessage(caught, signupMode ? '가입하지 못했습니다.' : '로그인하지 못했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="page narrow">
      <h1>{signupMode ? '가입' : '로그인'}</h1>
      {expiredReason && !signupMode && <p className="notice">{expiredReason}</p>}

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
        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
        <button className="button" type="submit" disabled={submitting}>
          {signupMode ? '가입하고 로그인' : '로그인'}
        </button>
      </form>

      <button
        type="button"
        className="link"
        onClick={() => setParams(signupMode ? {} : { mode: 'signup' }, { replace: true })}
      >
        {signupMode ? '이미 계정이 있어요' : '처음이에요 (가입)'}
      </button>
    </main>
  )
}
