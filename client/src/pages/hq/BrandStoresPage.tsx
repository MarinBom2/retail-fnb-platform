import { useCallback, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { storePath } from '../../routes'
import { hqApi } from '../../shared/api/hq/hqService'
import type { HqStore, StaffRoleCode, StoreStaffItem } from '../../shared/api/hq/hqService'
import { useCurrentBrand } from '../../shared/hq/hqContext'
import { useResource } from '../../shared/hooks/useResource'
import { Badge } from '../../shared/ui/Badge'
import { useConfirm } from '../../shared/ui/confirmContext'
import { Dialog } from '../../shared/ui/Dialog'
import { EmptyState, LoadState } from '../../shared/ui/EmptyState'
import { IconPlus, IconRefresh, IconTrash } from '../../shared/ui/icons'
import { Menu } from '../../shared/ui/Menu'
import type { MenuItem } from '../../shared/ui/Menu'
import { useToast } from '../../shared/ui/toastContext'
import { formatNumber } from '../../shared/utils/format'
import { hqErrorMessage, nextCode } from './hqUtils'
import './hq.css'

const ROLES: { code: StaffRoleCode; label: string }[] = [
  { code: 'OWNER', label: '점주' },
  { code: 'MANAGER', label: '매니저' },
  { code: 'STAFF', label: '직원' },
]

/** SCR-11 본사 매장 관리: 매장 추가·영업 시작/종료·이름·직원 배정 (FR-ST-002, FR-ST-003) */
export default function BrandStoresPage() {
  const { brand, reloadBrands } = useCurrentBrand()
  const navigate = useNavigate()
  const toast = useToast()
  const confirm = useConfirm()
  const loader = useCallback(() => hqApi.stores(brand.brandId), [brand.brandId])
  const { data, loading, error, reload, replace } = useResource(loader)

  const [creating, setCreating] = useState(false)
  const [renaming, setRenaming] = useState<HqStore | null>(null)
  const [staffOf, setStaffOf] = useState<HqStore | null>(null)

  const updateRow = (updated: HqStore) => replace((data ?? []).map((s) => (s.storeId === updated.storeId ? updated : s)))

  const open = async (store: HqStore) => {
    try {
      updateRow(await hqApi.openStore(brand.brandId, store.storeId))
      toast.show(`${store.name} 영업을 시작했습니다.`, 'success')
      reloadBrands()
    } catch (caught) {
      toast.error(caught, '영업을 시작하지 못했습니다.')
    }
  }

  const close = async (store: HqStore) => {
    const ok = await confirm({
      title: `${store.name} 영업을 종료할까요?`,
      message: '영업 종료 중에는 이 매장에서 새 주문을 받을 수 없습니다. 언제든 다시 시작할 수 있습니다.',
      confirmLabel: '영업 종료',
      tone: 'danger',
    })
    if (!ok) return
    try {
      updateRow(await hqApi.closeStore(brand.brandId, store.storeId))
      toast.show(`${store.name} 영업을 종료했습니다.`, 'success')
      reloadBrands()
    } catch (caught) {
      toast.error(caught, '영업을 종료하지 못했습니다.')
    }
  }

  const stores = data ?? []
  const openCount = stores.filter((s) => s.status.code === 'OPEN').length

  return (
    <div className="page-content">
      <header className="page-header">
        <div>
          <h1>매장 관리</h1>
          <p className="muted">
            {brand.name} · 매장 {formatNumber(stores.length)}곳 · 영업 중 {formatNumber(openCount)}
          </p>
        </div>
        <div className="header-actions">
          <button type="button" className="button secondary small" onClick={reload} disabled={loading}>
            <IconRefresh className="button-icon" />
            새로고침
          </button>
          <button type="button" className="button small" onClick={() => setCreating(true)}>
            <IconPlus className="button-icon" />
            매장 추가
          </button>
        </div>
      </header>

      {!data && <LoadState loading={loading} error={error} onRetry={reload} />}

      {data && data.length === 0 && (
        <EmptyState
          title="아직 매장이 없습니다."
          action={
            <button type="button" className="button" onClick={() => setCreating(true)}>
              첫 매장 추가하기
            </button>
          }
        >
          매장 코드와 이름만 있으면 됩니다. 추가한 뒤 영업을 시작하고 직원을 배정하세요.
        </EmptyState>
      )}

      {data && data.length > 0 && (
        <div className="table-wrap">
          <table className="data-table responsive hq-table">
            <thead>
              <tr>
                <th scope="col">매장</th>
                <th scope="col">상태</th>
                <th scope="col" className="num">직원</th>
                <th scope="col" className="actions-col">
                  <span className="visually-hidden">작업</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {stores.map((store) => {
                const isOpen = store.status.code === 'OPEN'
                const items: MenuItem[] = [
                  { label: '직원 관리', onSelect: () => setStaffOf(store) },
                  { label: '이름 바꾸기', onSelect: () => setRenaming(store) },
                  { label: '매장 화면 열기', onSelect: () => navigate(storePath(store.storeId)) },
                  ...(isOpen ? [{ label: '영업 종료', onSelect: () => void close(store), danger: true }] : []),
                ]
                return (
                  <tr key={store.storeId}>
                    <td data-label="매장">
                      <span className="hq-name">
                        <strong>{store.name}</strong>
                        <span className="small">{store.code}</span>
                      </span>
                    </td>
                    <td data-label="상태">
                      <Badge tone={isOpen ? 'success' : store.status.code === 'PREPARING' ? 'info' : 'neutral'}>
                        {store.status.description}
                      </Badge>
                    </td>
                    <td data-label="직원" className="num">
                      <button type="button" className="link" onClick={() => setStaffOf(store)}>
                        {formatNumber(store.staffCount)}명
                      </button>
                    </td>
                    <td className="row-actions">
                      {!isOpen && (
                        <button type="button" className="button small" onClick={() => void open(store)}>
                          영업 시작
                        </button>
                      )}
                      <Menu label={`${store.name} 더 보기`} items={items} />
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {creating && (
        <CreateStoreDialog
          existingCodes={stores.map((s) => s.code)}
          onClose={() => setCreating(false)}
          onCreated={async (store) => {
            setCreating(false)
            replace([...stores, store].sort((a, b) => a.code.localeCompare(b.code)))
            reloadBrands()
            const start = await confirm({
              title: `${store.name}을(를) 추가했습니다`,
              message: '바로 영업을 시작할까요? 영업 중이어야 POS 에서 주문을 받을 수 있습니다.',
              confirmLabel: '영업 시작',
            })
            if (start) await open(store)
          }}
        />
      )}
      {renaming && (
        <RenameStoreDialog
          store={renaming}
          onClose={() => setRenaming(null)}
          onSaved={(updated) => {
            updateRow(updated)
            setRenaming(null)
          }}
        />
      )}
      {staffOf && (
        <StaffDialog
          key={staffOf.storeId}
          store={staffOf}
          onClose={() => setStaffOf(null)}
          onCountChange={(count) => updateRow({ ...staffOf, staffCount: count })}
        />
      )}
    </div>
  )
}

function CreateStoreDialog({
  existingCodes,
  onClose,
  onCreated,
}: {
  existingCodes: string[]
  onClose: () => void
  onCreated: (store: HqStore) => void
}) {
  const { brand } = useCurrentBrand()
  const [code, setCode] = useState(() => nextCode('S', existingCodes))
  const [name, setName] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const valid = code.trim().length > 0 && name.trim().length > 0

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!valid) return
    setBusy(true)
    setError(null)
    try {
      onCreated(await hqApi.createStore(brand.brandId, code.trim(), name.trim()))
    } catch (caught) {
      setError(hqErrorMessage(caught, '매장을 추가하지 못했습니다.', { ALREADY_EXIST: '이미 쓰는 매장 코드입니다.' }))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open title="매장 추가" description="추가하면 오픈 준비 상태가 됩니다." size="sm" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <label>
          매장 이름
          <input value={name} maxLength={100} required placeholder="예: 강남점" onChange={(event) => setName(event.target.value)} />
        </label>
        <label>
          매장 코드 (브랜드 안에서 하나)
          <input value={code} maxLength={30} required onChange={(event) => setCode(event.target.value)} />
        </label>
        {error && (
          <p className="field-error" role="alert">
            {error}
          </p>
        )}
        <div className="dialog-actions">
          <button type="button" className="button secondary" onClick={onClose}>
            닫기
          </button>
          <button type="submit" className="button" disabled={!valid || busy}>
            {busy ? '추가 중…' : '추가'}
          </button>
        </div>
      </form>
    </Dialog>
  )
}

function RenameStoreDialog({ store, onClose, onSaved }: { store: HqStore; onClose: () => void; onSaved: (store: HqStore) => void }) {
  const { brand } = useCurrentBrand()
  const toast = useToast()
  const [name, setName] = useState(store.name)
  const [busy, setBusy] = useState(false)
  const trimmed = name.trim()

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!trimmed) return
    setBusy(true)
    try {
      onSaved(await hqApi.renameStore(brand.brandId, store.storeId, trimmed))
      toast.show('매장 이름을 바꿨습니다.', 'success')
    } catch (caught) {
      toast.error(caught, '이름을 바꾸지 못했습니다.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open title="매장 이름 바꾸기" description={store.code} size="sm" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <label>
          매장 이름
          <input value={name} maxLength={100} required onChange={(event) => setName(event.target.value)} />
        </label>
        <div className="dialog-actions">
          <button type="button" className="button secondary" onClick={onClose}>
            닫기
          </button>
          <button type="submit" className="button" disabled={!trimmed || trimmed === store.name || busy}>
            {busy ? '저장 중…' : '저장'}
          </button>
        </div>
      </form>
    </Dialog>
  )
}

/** 매장 직원: 목록, 역할 바꾸기, 빼기, 이메일로 배정 (가입한 회원만) */
function StaffDialog({
  store,
  onClose,
  onCountChange,
}: {
  store: HqStore
  onClose: () => void
  onCountChange: (count: number) => void
}) {
  const { brand } = useCurrentBrand()
  const toast = useToast()
  const confirm = useConfirm()
  const loader = useCallback(() => hqApi.staff(brand.brandId, store.storeId), [brand.brandId, store.storeId])
  const { data, loading, error, reload, replace } = useResource(loader)

  const [email, setEmail] = useState('')
  const [role, setRole] = useState<StaffRoleCode>('STAFF')
  const [busy, setBusy] = useState(false)
  const [addError, setAddError] = useState<string | null>(null)

  const setList = (list: StoreStaffItem[]) => {
    replace(list)
    onCountChange(list.length)
  }

  const add = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const trimmed = email.trim()
    if (!trimmed) return
    setBusy(true)
    setAddError(null)
    try {
      const added = await hqApi.addStaff(brand.brandId, store.storeId, trimmed, role)
      setList([...(data ?? []), added])
      setEmail('')
      toast.show(`${added.email} 님을 ${added.role.description}(으)로 배정했습니다.`, 'success')
    } catch (caught) {
      setAddError(
        hqErrorMessage(caught, '배정하지 못했습니다.', {
          NOT_FOUND_MEMBER: '가입한 회원만 배정할 수 있습니다. 먼저 가입을 안내해 주세요.',
          ALREADY_EXIST: '이미 이 매장에 배정된 회원입니다.',
        }),
      )
    } finally {
      setBusy(false)
    }
  }

  const changeRole = async (item: StoreStaffItem, next: StaffRoleCode) => {
    try {
      const updated = await hqApi.changeStaffRole(brand.brandId, store.storeId, item.staffId, next)
      setList((data ?? []).map((s) => (s.staffId === updated.staffId ? updated : s)))
      toast.show(`${updated.email} 님의 역할을 ${updated.role.description}(으)로 바꿨습니다.`, 'success')
    } catch (caught) {
      toast.error(caught, '역할을 바꾸지 못했습니다.')
    }
  }

  const remove = async (item: StoreStaffItem) => {
    const ok = await confirm({
      title: `${item.email} 님을 이 매장에서 뺄까요?`,
      message: '이 매장 화면에 더 이상 들어올 수 없습니다. 계정은 그대로 남습니다.',
      confirmLabel: '빼기',
      tone: 'danger',
    })
    if (!ok) return
    try {
      await hqApi.removeStaff(brand.brandId, store.storeId, item.staffId)
      setList((data ?? []).filter((s) => s.staffId !== item.staffId))
      toast.show('매장에서 뺐습니다.', 'success')
    } catch (caught) {
      toast.error(caught, '빼지 못했습니다.')
    }
  }

  return (
    <Dialog open title="직원 관리" description={`${store.name} · 점주·매니저는 재고를 바꿀 수 있고, 직원은 판매만 합니다.`} onClose={onClose}>
      {!data && <LoadState loading={loading} error={error} onRetry={reload} />}
      {data && data.length === 0 && <p className="muted small">아직 배정된 직원이 없습니다.</p>}
      {data && data.length > 0 && (
        <ul className="staff-list">
          {data.map((item) => (
            <li key={item.staffId}>
              <span className="email">{item.email}</span>
              <label className="visually-hidden" htmlFor={`role-${item.staffId}`}>
                {item.email} 역할
              </label>
              <select
                id={`role-${item.staffId}`}
                value={item.role.code}
                onChange={(event) => void changeRole(item, event.target.value as StaffRoleCode)}
              >
                {ROLES.map((option) => (
                  <option key={option.code} value={option.code}>
                    {option.label}
                  </option>
                ))}
              </select>
              <button type="button" className="icon-button" aria-label={`${item.email} 빼기`} onClick={() => void remove(item)}>
                <IconTrash />
              </button>
            </li>
          ))}
        </ul>
      )}

      <form className="inline-form" onSubmit={add}>
        <label className="form-label">
          <span className="small muted">이메일로 배정</span>
          <input
            type="email"
            inputMode="email"
            autoComplete="off"
            placeholder="staff@example.com"
            value={email}
            required
            onChange={(event) => setEmail(event.target.value)}
          />
        </label>
        <label className="visually-hidden" htmlFor="new-staff-role">
          역할
        </label>
        <select id="new-staff-role" value={role} onChange={(event) => setRole(event.target.value as StaffRoleCode)}>
          {ROLES.map((option) => (
            <option key={option.code} value={option.code}>
              {option.label}
            </option>
          ))}
        </select>
        <button type="submit" className="button" disabled={!email.trim() || busy}>
          {busy ? '배정 중…' : '배정'}
        </button>
      </form>
      {addError && (
        <p className="field-error" role="alert">
          {addError}
        </p>
      )}
    </Dialog>
  )
}
