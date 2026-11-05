import { ApiError } from '../../shared/core/apiClient'
import { errorMessage } from '../../shared/utils/errorMessage'

/** 서버 code 별로 본사 화면에 맞는 문장. 그 밖은 서버 문장 그대로 */
export function hqErrorMessage(caught: unknown, fallback: string, overrides: Record<string, string> = {}) {
  if (caught instanceof ApiError && caught.code && overrides[caught.code]) {
    return overrides[caught.code]
  }
  return errorMessage(caught, fallback)
}

/** 이미 쓰는 코드와 겹치지 않는 다음 번호 (예: SKU-0013). 사람이 고칠 수 있는 제안값일 뿐이다 */
export function nextCode(prefix: string, existing: Iterable<string>, width = 3) {
  const taken = new Set(existing)
  let max = 0
  for (const code of taken) {
    const match = code.startsWith(`${prefix}-`) ? /^(\d+)$/.exec(code.slice(prefix.length + 1)) : null
    if (match) max = Math.max(max, Number(match[1]))
  }
  let n = max + 1
  while (taken.has(`${prefix}-${String(n).padStart(width, '0')}`)) n += 1
  return `${prefix}-${String(n).padStart(width, '0')}`
}

/** 색상·사이즈는 대문자, 앞뒤 공백 없이 (서버 ApparelOption 과 같은 규칙) */
export const normalizeOption = (value: string) => value.trim().toUpperCase()

/** SKU 코드에 넣을 수 있게 공백을 하이픈으로 */
export const codePart = (value: string) => normalizeOption(value).replace(/\s+/g, '-')
