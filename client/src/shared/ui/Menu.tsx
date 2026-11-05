import { useEffect, useId, useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'
import { IconMore } from './icons'

export type MenuItem = { label: string; onSelect: () => void; danger?: boolean }

/** 넘침 메뉴(⋯). 버튼 하나에 항목 목록. Esc·바깥 클릭으로 닫히고 화살표로 옮긴다 */
export function Menu({ label, items }: { label: string; items: MenuItem[] }) {
  const [open, setOpen] = useState(false)
  const anchor = useRef<HTMLDivElement>(null)
  const listId = useId()

  useEffect(() => {
    if (!open) return
    const onDown = (event: MouseEvent) => {
      if (!anchor.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onDown)
    return () => document.removeEventListener('mousedown', onDown)
  }, [open])

  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'Escape') {
      setOpen(false)
      anchor.current?.querySelector('button')?.focus()
      return
    }
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      const buttons = Array.from(anchor.current?.querySelectorAll<HTMLButtonElement>('.menu-popover button') ?? [])
      if (buttons.length === 0) return
      const index = buttons.indexOf(document.activeElement as HTMLButtonElement)
      const next = event.key === 'ArrowDown' ? (index + 1) % buttons.length : (index - 1 + buttons.length) % buttons.length
      buttons[next].focus()
    }
  }

  return (
    <div className="menu-anchor" ref={anchor} onKeyDown={onKeyDown}>
      <button
        type="button"
        className="icon-button"
        aria-label={label}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={listId}
        onClick={() => setOpen((v) => !v)}
      >
        <IconMore />
      </button>
      {open && (
        <ul className="menu-popover" role="menu" id={listId}>
          {items.map((item) => (
            <li key={item.label} role="none">
              <button
                type="button"
                role="menuitem"
                className={item.danger ? 'danger' : ''}
                onClick={() => {
                  setOpen(false)
                  item.onSelect()
                }}
              >
                {item.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
