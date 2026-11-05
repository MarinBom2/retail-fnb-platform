import { useEffect, useId, useRef } from 'react'
import type { ReactNode } from 'react'

type DialogProps = {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
  /** 버튼 영역 */
  footer?: ReactNode
  size?: 'sm' | 'md' | 'lg'
  description?: string
}

/**
 * 네이티브 dialog 의 showModal 을 쓴다: 포커스 가두기, Esc 닫기, 바깥 비활성화를 브라우저가 맡는다.
 * 열릴 때 첫 입력칸(없으면 첫 버튼)에 포커스가 간다.
 */
export function Dialog({ open, title, onClose, children, footer, size = 'md', description }: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) {
      dialog.showModal()
      const first = dialog.querySelector<HTMLElement>('input, select, textarea, [data-autofocus]')
      first?.focus()
    } else if (!open && dialog.open) {
      dialog.close()
    }
  }, [open])

  return (
    <dialog
      ref={ref}
      className={`dialog dialog-${size}`}
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault()
        onClose()
      }}
      onClick={(event) => {
        // 바깥(backdrop) 클릭으로 닫기
        if (event.target === ref.current) onClose()
      }}
    >
      {open && (
        <div className="dialog-panel">
          <header className="dialog-header">
            <div>
              <h2 id={titleId}>{title}</h2>
              {description && <p className="muted small">{description}</p>}
            </div>
            <button type="button" className="icon-button" aria-label="닫기" onClick={onClose}>
              ×
            </button>
          </header>
          <div className="dialog-body">{children}</div>
          {footer && <footer className="dialog-footer">{footer}</footer>}
        </div>
      )}
    </dialog>
  )
}
