import { useState } from 'react'
import type { KeyboardEvent } from 'react'
import { IconClose } from '../../shared/ui/icons'
import { normalizeOption } from './hqUtils'

/** 값을 입력하고 Enter(또는 쉼표)로 칩을 만든다. 같은 값은 한 번만 */
export function ChipInput({
  id,
  values,
  onChange,
  placeholder,
}: {
  id: string
  values: string[]
  onChange: (values: string[]) => void
  placeholder?: string
}) {
  const [draft, setDraft] = useState('')

  const commit = (raw: string) => {
    const parts = raw.split(',').map(normalizeOption).filter(Boolean)
    if (parts.length === 0) return
    onChange([...values, ...parts.filter((part, i) => !values.includes(part) && parts.indexOf(part) === i)])
    setDraft('')
  }

  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault()
      commit(draft)
    } else if (event.key === 'Backspace' && draft === '' && values.length > 0) {
      onChange(values.slice(0, -1))
    }
  }

  return (
    <div className="chip-input">
      {values.map((value) => (
        <span key={value} className="chip-token">
          {value}
          <button type="button" aria-label={`${value} 빼기`} onClick={() => onChange(values.filter((v) => v !== value))}>
            <IconClose />
          </button>
        </span>
      ))}
      <input
        id={id}
        value={draft}
        placeholder={values.length === 0 ? placeholder : ''}
        maxLength={30}
        onChange={(event) => setDraft(event.target.value)}
        onKeyDown={onKeyDown}
        onBlur={() => commit(draft)}
      />
    </div>
  )
}
