import { createContext, use } from 'react'

export type ConfirmOptions = {
  title: string
  message?: string
  confirmLabel?: string
  tone?: 'primary' | 'danger'
  /** 값을 받아야 할 때 (예: 거절 사유). 있으면 입력칸을 보여 주고 입력값을 돌려준다 */
  input?: { label: string; placeholder?: string; required?: boolean }
}

/** 확인하면 true(입력이 있으면 입력값), 닫으면 false */
export type ConfirmFn = (options: ConfirmOptions) => Promise<boolean | string>

export const ConfirmContext = createContext<ConfirmFn | null>(null)

export function useConfirm() {
  const context = use(ConfirmContext)
  if (!context) throw new Error('useConfirm 은 ConfirmProvider 안에서만 쓸 수 있습니다.')
  return context
}
