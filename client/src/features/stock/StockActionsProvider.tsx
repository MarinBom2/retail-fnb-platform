import { useCallback, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { AvailabilityDialog } from './AvailabilityDialog'
import { TransferRequestDialog } from './TransferRequestDialog'
import { StockActionsContext } from './stockActionsContext'
import type { AvailabilityRequest, TransferPrefill } from './stockActionsContext'

/** 재고 부족 → 다른 매장 재고 → 이동 요청 흐름을 화면 어디서나 같은 대화상자로 연다 */
export function StockActionsProvider({ children }: { children: ReactNode }) {
  const [transfer, setTransfer] = useState<{ key: number; prefill: TransferPrefill } | null>(null)
  const [availability, setAvailability] = useState<{ key: number; request: AvailabilityRequest } | null>(null)
  const [transferVersion, setTransferVersion] = useState(0)

  const openTransferRequest = useCallback((prefill: TransferPrefill = {}) => {
    setAvailability(null)
    setTransfer({ key: Date.now(), prefill })
  }, [])

  const openAvailability = useCallback((request: AvailabilityRequest) => {
    setAvailability({ key: Date.now(), request })
  }, [])

  const value = useMemo(
    () => ({ openTransferRequest, openAvailability, transferVersion }),
    [openTransferRequest, openAvailability, transferVersion],
  )

  return (
    <StockActionsContext value={value}>
      {children}
      {availability && (
        <AvailabilityDialog
          key={availability.key}
          request={availability.request}
          onClose={() => setAvailability(null)}
          onRequestTransfer={openTransferRequest}
        />
      )}
      {transfer && (
        <TransferRequestDialog
          key={transfer.key}
          prefill={transfer.prefill}
          onClose={() => setTransfer(null)}
          onCreated={() => {
            setTransfer(null)
            setTransferVersion((v) => v + 1)
          }}
        />
      )}
    </StockActionsContext>
  )
}
