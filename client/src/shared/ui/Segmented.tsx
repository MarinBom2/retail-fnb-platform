import type { ReactNode } from 'react'

export type SegmentOption<T extends string> = { value: T; label: ReactNode }

type Props<T extends string> = {
  label: string
  value: T
  options: SegmentOption<T>[]
  onChange: (value: T) => void
  block?: boolean
}

/** iOS 세그먼트 컨트롤. 탭(role=tablist)으로 읽힌다 */
export function Segmented<T extends string>({ label, value, options, onChange, block }: Props<T>) {
  return (
    <div className={`segmented ${block ? 'block' : ''}`} role="tablist" aria-label={label}>
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          role="tab"
          aria-selected={value === option.value}
          className={value === option.value ? 'selected' : ''}
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}
