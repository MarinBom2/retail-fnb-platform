/** 3D 를 띄우지 않을 때(모바일·움직임 줄이기·로딩 중) 같은 장면을 정적 그림으로 보여 준다 */
export function HeroFallback() {
  return (
    <svg className="hero-fallback" viewBox="0 0 640 320" role="img" aria-label="세 매장 사이로 재고 상자가 이동하는 그림">
      <defs>
        <linearGradient id="hf-floor" x1="0" x2="0" y1="0" y2="1">
          <stop offset="0" stopColor="#ffffff" stopOpacity="0" />
          <stop offset="1" stopColor="#000000" stopOpacity="0.06" />
        </linearGradient>
      </defs>
      <ellipse cx="320" cy="250" rx="280" ry="38" fill="url(#hf-floor)" />
      {/* 왼쪽: 생활용품 */}
      <g transform="translate(110 150)">
        <rect x="-60" y="0" width="120" height="86" rx="16" fill="#0071e3" />
        <rect x="-55" y="-10" width="110" height="12" rx="6" fill="#fff" />
      </g>
      {/* 가운데: 의류 */}
      <g transform="translate(320 128)">
        <rect x="-68" y="0" width="136" height="108" rx="18" fill="#af52de" />
        <rect x="-62" y="-10" width="124" height="12" rx="6" fill="#fff" />
      </g>
      {/* 오른쪽: 외식 */}
      <g transform="translate(530 160)">
        <rect x="-56" y="0" width="112" height="76" rx="16" fill="#ff9500" />
        <rect x="-51" y="-10" width="102" height="12" rx="6" fill="#fff" />
      </g>
      {/* 이동 경로와 상자 */}
      <path d="M320 112 Q 215 20 110 132" fill="none" stroke="#0a84ff" strokeWidth="2" strokeDasharray="4 6" opacity="0.6" />
      <path d="M530 140 Q 425 40 320 112" fill="none" stroke="#ff9f0a" strokeWidth="2" strokeDasharray="4 6" opacity="0.6" />
      <rect x="200" y="48" width="26" height="26" rx="6" fill="#0a84ff" transform="rotate(-12 213 61)" />
      <rect x="415" y="66" width="24" height="24" rx="6" fill="#ff9f0a" transform="rotate(14 427 78)" />
    </svg>
  )
}
