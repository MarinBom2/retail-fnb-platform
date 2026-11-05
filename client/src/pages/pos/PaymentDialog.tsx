import { useState } from 'react'
import type { PaymentMethodCode } from '../../shared/api/order/orderService'
import { Dialog } from '../../shared/ui/Dialog'
import { formatWon } from '../../shared/utils/format'

const METHODS: { code: PaymentMethodCode; label: string }[] = [
  { code: 'CARD', label: '카드' },
  { code: 'CASH', label: '현금' },
  { code: 'EASY_PAY', label: '간편결제' },
]

type Props = {
  total: number
  title?: string
  busy: boolean
  onClose: () => void
  onPay: (method: PaymentMethodCode) => void
}

/** 결제 수단 고르기. 현금은 받은 금액으로 거스름돈만 계산한다 (서버에는 합계를 보낸다) */
export function PaymentDialog({ total, title = '결제', busy, onClose, onPay }: Props) {
  const [method, setMethod] = useState<PaymentMethodCode>('CARD')
  const [received, setReceived] = useState('')
  const receivedAmount = Number(received)
  const change = method === 'CASH' && received ? receivedAmount - total : null

  return (
    <Dialog open title={title} size="sm" onClose={onClose}>
      <form
        className="form"
        onSubmit={(event) => {
          event.preventDefault()
          if (method === 'CASH' && received && receivedAmount < total) return
          onPay(method)
        }}
      >
        <div className="pay-total">
          <span className="muted">결제할 금액</span>
          <strong>{formatWon(total)}</strong>
        </div>
        <div className="segmented" role="radiogroup" aria-label="결제 수단">
          {METHODS.map((item) => (
            <label key={item.code} className={method === item.code ? 'selected' : ''}>
              <input
                type="radio"
                name="payment-method"
                value={item.code}
                checked={method === item.code}
                onChange={() => setMethod(item.code)}
              />
              {item.label}
            </label>
          ))}
        </div>
        {method === 'CASH' && (
          <label>
            받은 금액 (선택)
            <input
              type="number"
              inputMode="numeric"
              min={0}
              step={100}
              value={received}
              placeholder={String(total)}
              onChange={(event) => setReceived(event.target.value)}
            />
          </label>
        )}
        {change !== null && (
          <p className={change < 0 ? 'error' : 'pay-change'}>
            {change < 0 ? `${formatWon(-change)} 부족합니다` : `거스름돈 ${formatWon(change)}`}
          </p>
        )}
        <div className="dialog-actions">
          <button type="button" className="button secondary" onClick={onClose} disabled={busy}>
            닫기
          </button>
          <button type="submit" className="button large" disabled={busy}>
            {busy ? '결제 중…' : `${formatWon(total)} 결제`}
          </button>
        </div>
      </form>
    </Dialog>
  )
}
