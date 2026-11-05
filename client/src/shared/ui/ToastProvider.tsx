import { useCallback, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { errorMessage } from '../utils/errorMessage'
import { IconClose } from './icons'
import { ToastContext } from './toastContext'
import type { ToastTone } from './toastContext'

type ToastItem = { id: number; message: string; tone: ToastTone }

let nextId = 1

/** 위쪽 가운데 캡슐 알림. 성공·안내는 3.5초, 오류는 6초 뒤 사라진다 */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const dismiss = useCallback((id: number) => setItems((list) => list.filter((item) => item.id !== id)), [])

  const show = useCallback(
    (message: string, tone: ToastTone = 'success') => {
      const id = nextId++
      setItems((list) => [...list.slice(-2), { id, message, tone }])
      window.setTimeout(() => dismiss(id), tone === 'error' ? 6000 : 3500)
    },
    [dismiss],
  )

  const error = useCallback((caught: unknown, fallback: string) => show(errorMessage(caught, fallback), 'error'), [show])

  const value = useMemo(() => ({ show, error }), [show, error])

  return (
    <ToastContext value={value}>
      {children}
      <div className="toast-region" aria-live="polite" aria-atomic="false">
        {items.map((item) => (
          <div key={item.id} className={`toast toast-${item.tone}`} role={item.tone === 'error' ? 'alert' : 'status'}>
            <span>{item.message}</span>
            <button type="button" className="icon-button" aria-label="알림 닫기" onClick={() => dismiss(item.id)}>
              <IconClose width={14} height={14} />
            </button>
          </div>
        ))}
      </div>
    </ToastContext>
  )
}
