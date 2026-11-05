import { Link } from 'react-router-dom'
import { ROUTES } from '../../routes'
import { useAuth } from '../../shared/auth/useAuth'

/** SCR-01 홈. 로그인 상태만 보여 주는 뼈대 */
export default function HomePage() {
  const { status, member, logout } = useAuth()

  return (
    <main className="page">
      <h1>매장 관리</h1>
      <p className="muted">생활용품 · 의류 · 외식 매장을 한곳에서 관리합니다.</p>

      {status === 'checking' && <p aria-busy="true">로그인 상태를 확인하고 있습니다.</p>}
      {status === 'anonymous' && (
        <Link className="button" to={ROUTES.SCR02_LOGIN}>
          로그인
        </Link>
      )}
      {status === 'authenticated' && member && (
        <section className="card">
          <p>
            {member.email} ({member.roleType.description})
          </p>
          <button type="button" className="button secondary" onClick={() => void logout()}>
            로그아웃
          </button>
        </section>
      )}
    </main>
  )
}
