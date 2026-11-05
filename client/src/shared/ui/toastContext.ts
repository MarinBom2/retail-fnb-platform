import { createContext, use } from 'react'

export type ToastTone = 'success' | 'error' | 'info'

export type ToastApi = {
  show: (message: string, tone?: ToastTone) => void
  /** 실패를 사람이 읽는 문장으로 (errorMessage) */
  error: (caught: unknown, fallback: string) => void
}

export const ToastContext = createContext<ToastApi | null>(null)

export function useToast() {
  const context = use(ToastContext)
  if (!context) throw new Error('useToast 는 ToastProvider 안에서만 쓸 수 있습니다.')
  return context
}
