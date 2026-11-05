import { Suspense, lazy, useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { ROUTES, demoLoginPath, storePath } from '../../routes'
import { useAuth } from '../../shared/auth/useAuth'
import { IconBag, IconBoxes, IconGrid, IconReceipt, IconStore, IconTransfer } from '../../shared/ui/icons'
import { readLastStoreId } from '../../shared/utils/storage'
import { HeroFallback } from './HeroFallback'
import './landing.css'

// three 는 이 청크에만 들어간다
const HeroScene = lazy(() => import('./HeroScene'))

const FEATURES = [
  {
    icon: <IconTransfer />,
    title: '매장 간 재고 이동',
    body: '요청 → 승인 → 출고 → 입고 네 단계로 매장끼리 재고를 주고받습니다. 단계마다 누가 처리할지 정해져 있고, 출고·입고 때 재고가 자동으로 맞춰집니다.',
    wide: true,
  },
  {
    icon: <IconBag />,
    title: '품절이어도 판매를 놓치지 않게',
    body: 'POS 에서 재고가 모자라면 같은 브랜드의 다른 매장 재고를 바로 보여 주고, 그 자리에서 이동을 요청할 수 있습니다.',
  },
  {
    icon: <IconBoxes />,
    title: '안전재고 알림과 매장 추천',
    body: '품목마다 안전재고를 정해 두면 대시보드에 부족 품목이 뜨고, 재고가 가장 많은 매장을 함께 추천합니다.',
  },
  {
    icon: <IconStore />,
    title: '업종에 맞춘 POS',
    body: '생활용품은 바코드 스캔, 의류는 색상 × 사이즈 표, 외식은 테이블 주문과 포장. 같은 플랫폼에서 업종별 화면으로 일합니다.',
  },
  {
    icon: <IconReceipt />,
    title: '레시피로 식자재 자동 차감',
    body: '메뉴에 레시피를 등록하면 결제할 때 식자재가 그만큼 빠집니다. 재고가 모자라면 결제 전에 알려 줍니다.',
  },
  {
    icon: <IconGrid />,
    title: '매장 대시보드',
    body: '오늘 매출·객단가·시간대별 매출, 많이 팔린 상품, 처리할 이동 건을 한 화면에서 봅니다.',
    wide: true,
  },
]

const INDUSTRIES = [
  { name: '생활용품 리테일', tone: 'gm', points: ['바코드 계산', '단일 SKU 대량 품목', '안전재고 관리'] },
  { name: '의류 리테일', tone: 'apparel', points: ['스타일 × 색상 × 사이즈 SKU', '사이즈 재고 표', '매장 간 사이즈 이동'] },
  { name: '외식', tone: 'food', points: ['테이블·포장 주문', '메뉴와 식자재 분리', '레시피 기반 차감'] },
]

const STEPS = [
  { label: '요청', who: '받을 매장', desc: '모자란 품목을 재고가 있는 매장에 요청' },
  { label: '승인', who: '보낼 매장', desc: '보낼 수 있는지 확인하고 승인 또는 거절' },
  { label: '출고', who: '보낼 매장', desc: '출고하면 보낸 매장 재고가 빠짐' },
  { label: '입고', who: '받을 매장', desc: '입고를 확인하면 받은 매장 재고에 더해짐' },
]

/** 3D 를 띄울지: 넓은 화면이고 움직임 줄이기가 꺼져 있을 때만 */
function useShow3d() {
  const [show, setShow] = useState(false)
  useEffect(() => {
    const wide = window.matchMedia('(min-width: 720px)')
    const motion = window.matchMedia('(prefers-reduced-motion: reduce)')
    const update = () => setShow(wide.matches && !motion.matches)
    update()
    wide.addEventListener('change', update)
    motion.addEventListener('change', update)
    return () => {
      wide.removeEventListener('change', update)
      motion.removeEventListener('change', update)
    }
  }, [])
  return show
}

/** SCR-01 소개. 체인 본사·매장 운영 담당자에게 플랫폼을 소개하고 데모로 잇는다 */
export default function LandingPage() {
  const { status } = useAuth()
  const show3d = useShow3d()

  if (status === 'authenticated') {
    const last = readLastStoreId()
    return <Navigate to={last ? storePath(last) : ROUTES.SCR03_STORES} replace />
  }

  return (
    <div className="landing">
      <header className="landing-header">
        <div className="landing-header-inner">
          <span className="app-logo">
            <span className="logo-mark" aria-hidden="true" />
            <span className="logo-text">Retail F&amp;B</span>
          </span>
          <nav className="landing-nav" aria-label="소개">
            <a href="#features">기능</a>
            <a href="#flow">이동 흐름</a>
            <a href="#industries">업종</a>
          </nav>
          <Link className="button secondary small" to={ROUTES.SCR02_LOGIN}>
            로그인
          </Link>
        </div>
      </header>

      <section className="hero">
        <p className="eyebrow">멀티 브랜드 체인을 위한 매장 운영 플랫폼</p>
        <h1>
          매장이 다르면
          <br />
          재고도 따로였습니다.
        </h1>
        <p className="hero-lead">
          생활용품·의류·외식 매장을 하나의 재고 모델로. 품절이면 옆 매장 재고를 찾아 오늘 판매를 살립니다.
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

      <section className="hero-stage" aria-label="매장 간 재고 이동 장면">
        {show3d ? (
          <Suspense fallback={<HeroFallback />}>
            <HeroScene />
          </Suspense>
        ) : (
          <HeroFallback />
        )}
        <ul className="stage-legend" aria-hidden="true">
          <li>
            <span className="dot gm" /> 생활용품
          </li>
          <li>
            <span className="dot apparel" /> 의류
          </li>
          <li>
            <span className="dot food" /> 외식
          </li>
        </ul>
      </section>

      <section id="features" className="landing-section">
        <h2>매장 운영에 필요한 것만, 업종에 맞게</h2>
        <p className="section-lead">본사와 매장이 같은 데이터를 보고, 업종마다 다른 일하는 방식은 그대로 둡니다.</p>
        <div className="bento">
          {FEATURES.map((feature) => (
            <article key={feature.title} className={`bento-card ${feature.wide ? 'wide' : ''}`}>
              <span className="bento-icon" aria-hidden="true">
                {feature.icon}
              </span>
              <h3>{feature.title}</h3>
              <p>{feature.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section id="flow" className="landing-section">
        <h2>매장 간 이동, 네 단계</h2>
        <p className="section-lead">전화나 메신저가 아니라 장부에서 일어납니다. 단계마다 시각과 담당 매장이 남습니다.</p>
        <ol className="flow-timeline">
          {STEPS.map((step, index) => (
            <li key={step.label} className="flow-item">
              <span className="flow-index">{index + 1}</span>
              <div>
                <strong>{step.label}</strong>
                <span className="flow-who">{step.who}</span>
                <p>{step.desc}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      <section id="industries" className="landing-section">
        <h2>세 업종, 하나의 재고 모델</h2>
        <div className="industry-grid">
          {INDUSTRIES.map((industry) => (
            <article key={industry.name} className={`industry-card tone-${industry.tone}`}>
              <span className="industry-swatch" aria-hidden="true" />
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

      <footer className="landing-footer muted small">© Retail F&amp;B · 매장 운영 플랫폼</footer>
    </div>
  )
}
