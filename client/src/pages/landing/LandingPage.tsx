import { Link, Navigate } from 'react-router-dom'
import { ROUTES, demoLoginPath, storePath } from '../../routes'
import { useAuth } from '../../shared/auth/useAuth'
import { readLastStoreId } from '../../shared/utils/storage'
import './landing.css'

const FEATURES = [
  {
    title: '매장 간 재고 이동',
    body: '요청 → 승인 → 출고 → 입고 네 단계로 매장끼리 재고를 주고받습니다. 단계마다 누가 처리할지 정해져 있고, 출고·입고 때 재고가 자동으로 맞춰집니다.',
  },
  {
    title: '품절이어도 판매를 놓치지 않게',
    body: 'POS 에서 재고가 모자라면 같은 브랜드의 다른 매장 재고를 바로 보여 주고, 그 자리에서 이동을 요청할 수 있습니다.',
  },
  {
    title: '안전재고 알림과 매장 추천',
    body: '품목마다 안전재고를 정해 두면 대시보드에 부족 품목이 뜨고, 재고가 가장 많은 매장을 함께 추천합니다.',
  },
  {
    title: '업종에 맞춘 POS',
    body: '생활용품은 바코드 스캔, 의류는 색상 × 사이즈 표, 외식은 테이블 주문과 포장. 같은 플랫폼에서 업종별 화면으로 일합니다.',
  },
  {
    title: '레시피로 식자재 자동 차감',
    body: '메뉴에 레시피를 등록하면 결제할 때 식자재가 그만큼 빠집니다. 재고가 모자라면 결제 전에 알려 줍니다.',
  },
  {
    title: '매장 대시보드',
    body: '오늘 매출·객단가·시간대별 매출, 많이 팔린 상품, 처리할 이동 건을 한 화면에서 봅니다.',
  },
]

const INDUSTRIES = [
  { name: '생활용품 리테일', points: ['바코드 계산', '단일 SKU 대량 품목', '안전재고 관리'] },
  { name: '의류 리테일', points: ['스타일 × 색상 × 사이즈 SKU', '사이즈 재고 표', '매장 간 사이즈 이동'] },
  { name: '외식', points: ['테이블·포장 주문', '메뉴와 식자재 분리', '레시피 기반 차감'] },
]

/** SCR-01 소개. 체인 본사·매장 운영 담당자에게 플랫폼을 소개하고 데모로 잇는다 */
export default function LandingPage() {
  const { status } = useAuth()

  if (status === 'authenticated') {
    const last = readLastStoreId()
    return <Navigate to={last ? storePath(last) : ROUTES.SCR03_STORES} replace />
  }

  return (
    <div className="landing">
      <header className="landing-header">
        <span className="app-logo">
          <span className="logo-mark" aria-hidden="true" />
          <span className="logo-text">Retail F&B</span>
        </span>
        <Link className="button secondary small" to={ROUTES.SCR02_LOGIN}>
          로그인
        </Link>
      </header>

      <section className="hero">
        <p className="eyebrow">멀티 브랜드 체인을 위한 매장 운영 플랫폼</p>
        <h1>
          생활용품·의류·외식 매장을
          <br />
          하나의 플랫폼으로
        </h1>
        <p className="hero-lead">
          상품, 매장 재고, 매장 간 이동, 판매까지. 업종마다 다른 일하는 방식을 그대로 지원하면서 본사와 매장이 같은 데이터를
          봅니다.
        </p>
        <div className="hero-actions">
          <Link className="button large" to={demoLoginPath()}>
            데모로 체험하기
          </Link>
          <a className="button secondary large" href="#features">
            기능 보기
          </a>
        </div>
        <p className="muted small">데모 계정으로 세 업종의 가상 브랜드 매장을 바로 둘러볼 수 있습니다.</p>
      </section>

      <section className="flow-strip" aria-label="매장 간 이동 흐름">
        {['요청', '승인', '출고', '입고'].map((step, index) => (
          <div key={step} className="flow-step">
            <span className="flow-index">{index + 1}</span>
            <span>{step}</span>
          </div>
        ))}
      </section>

      <section id="features" className="landing-section">
        <h2>매장 운영에 필요한 것만, 업종에 맞게</h2>
        <div className="feature-grid">
          {FEATURES.map((feature) => (
            <article key={feature.title} className="feature-card">
              <h3>{feature.title}</h3>
              <p>{feature.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="landing-section">
        <h2>세 업종, 하나의 재고 모델</h2>
        <div className="industry-grid">
          {INDUSTRIES.map((industry) => (
            <article key={industry.name} className="industry-card">
              <h3>{industry.name}</h3>
              <ul>
                {industry.points.map((point) => (
                  <li key={point}>{point}</li>
                ))}
              </ul>
            </article>
          ))}
        </div>
      </section>

      <section className="landing-cta">
        <h2>지금 매장 화면을 직접 써 보세요</h2>
        <p>대시보드, POS, 재고, 매장 간 이동까지 데모 데이터로 모두 열려 있습니다.</p>
        <Link className="button large" to={demoLoginPath()}>
          데모로 체험하기
        </Link>
      </section>

      <footer className="landing-footer muted small">© Retail F&B · 매장 운영 플랫폼</footer>
    </div>
  )
}
