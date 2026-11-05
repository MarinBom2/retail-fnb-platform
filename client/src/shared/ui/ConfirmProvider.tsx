import { useCallback, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { Dialog } from './Dialog'
import { ConfirmContext } from './confirmContext'
import type { ConfirmOptions } from './confirmContext'

type Pending = ConfirmOptions & { resolve: (value: boolean | string) => void }

export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [pending, setPending] = useState<Pending | null>(null)
  const [value, setValue] = useState('')

  const confirm = useCallback(
    (options: ConfirmOptions) =>
      new Promise<boolean | string>((resolve) => {
        setValue('')
        setPending({ ...options, resolve })
      }),
    [],
  )

  const close = (result: boolean | string) => {
    pending?.resolve(result)
    setPending(null)
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (pending?.input) {
      if (pending.input.required && !value.trim()) return
      close(value.trim())
    } else {
      close(true)
    }
  }

  return (
    <ConfirmContext value={confirm}>
      {children}
      <Dialog open={pending !== null} title={pending?.title ?? ''} size="sm" onClose={() => close(false)}>
        {pending && (
          <form onSubmit={handleSubmit} className="form">
            {pending.message && <p>{pending.message}</p>}
            {pending.input && (
              <label>
                {pending.input.label}
                <input
                  value={value}
                  placeholder={pending.input.placeholder}
                  required={pending.input.required}
                  onChange={(event) => setValue(event.target.value)}
                />
              </label>
            )}
            <div className="dialog-actions">
              <button type="button" className="button secondary" onClick={() => close(false)}>
                닫기
              </button>
              <button
                type="submit"
                className={`button ${pending.tone === 'danger' ? 'danger' : ''}`}
                data-autofocus={pending.input ? undefined : true}
              >
                {pending.confirmLabel ?? '확인'}
              </button>
            </div>
          </form>
        )}
      </Dialog>
    </ConfirmContext>
  )
}
