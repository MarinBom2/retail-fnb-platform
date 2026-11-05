import type { SVGProps } from 'react'

type IconProps = SVGProps<SVGSVGElement>

/** 선 아이콘. 라이브러리 없이 필요한 것만 둔다 (stroke 1.75) */
const base = (props: IconProps) => ({
  width: 20,
  height: 20,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.75,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  'aria-hidden': true,
  ...props,
})

export const IconGrid = (p: IconProps) => (
  <svg {...base(p)}>
    <rect x="3.5" y="3.5" width="7" height="7" rx="2" />
    <rect x="13.5" y="3.5" width="7" height="7" rx="2" />
    <rect x="3.5" y="13.5" width="7" height="7" rx="2" />
    <rect x="13.5" y="13.5" width="7" height="7" rx="2" />
  </svg>
)

export const IconBag = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M5 8h14l-1 12H6L5 8z" />
    <path d="M9 8V6a3 3 0 0 1 6 0v2" />
  </svg>
)

export const IconReceipt = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M6 3h12v18l-2-1.5L14 21l-2-1.5L10 21l-2-1.5L6 21V3z" />
    <path d="M9 8h6M9 12h6M9 16h4" />
  </svg>
)

export const IconBoxes = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M4 7.5 12 3l8 4.5v9L12 21l-8-4.5v-9z" />
    <path d="M4 7.5 12 12l8-4.5M12 12v9" />
  </svg>
)

export const IconTransfer = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M4 8h13l-3-3M20 16H7l3 3" />
  </svg>
)

export const IconTag = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M3.5 12.5V4.5a1 1 0 0 1 1-1h8l8 8-8 8-9-7z" />
    <circle cx="8" cy="8" r="1.25" fill="currentColor" stroke="none" />
  </svg>
)

export const IconChevronRight = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="m9 6 6 6-6 6" />
  </svg>
)

export const IconSearch = (p: IconProps) => (
  <svg {...base(p)}>
    <circle cx="11" cy="11" r="6.5" />
    <path d="m20 20-4-4" />
  </svg>
)

export const IconClose = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M6 6l12 12M18 6 6 18" />
  </svg>
)

export const IconMore = (p: IconProps) => (
  <svg {...base(p)}>
    <circle cx="6" cy="12" r="1.25" fill="currentColor" stroke="none" />
    <circle cx="12" cy="12" r="1.25" fill="currentColor" stroke="none" />
    <circle cx="18" cy="12" r="1.25" fill="currentColor" stroke="none" />
  </svg>
)

export const IconRefresh = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M20 12a8 8 0 1 1-2.3-5.6" />
    <path d="M20 4v5h-5" />
  </svg>
)

export const IconPlus = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M12 5v14M5 12h14" />
  </svg>
)

export const IconCheck = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="m5 12.5 4.5 4.5L19 7.5" />
  </svg>
)

export const IconStore = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M4 10 5.5 4h13L20 10" />
    <path d="M4 10c0 1.5 1.3 2.5 2.7 2.5S9.3 11.5 9.3 10c0 1.5 1.2 2.5 2.7 2.5s2.7-1 2.7-2.5c0 1.5 1.2 2.5 2.6 2.5S20 11.5 20 10" />
    <path d="M5 12.5V20h14v-7.5M10 20v-5h4v5" />
  </svg>
)
