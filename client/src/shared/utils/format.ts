import type { CodeLabel } from '../types/common'

const wonFormat = new Intl.NumberFormat('ko-KR')

/** ₩19,900 */
export function formatWon(amount: number) {
  return `₩${wonFormat.format(amount)}`
}

export function formatNumber(value: number) {
  return wonFormat.format(value)
}

/** 수량 + 단위 (예: 1,200g, 3개) */
export function formatQuantity(quantity: number, unit: CodeLabel) {
  return `${wonFormat.format(quantity)}${unit.description}`
}

const kstDateTime = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

const kstTime = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

/** 서버 시각(UTC)을 한국 시간 "11. 05. 14:30" 으로 */
export function formatDateTimeKst(iso: string | null | undefined) {
  return iso ? kstDateTime.format(new Date(iso)) : '-'
}

export function formatTimeKst(iso: string | null | undefined) {
  return iso ? kstTime.format(new Date(iso)) : '-'
}

/** "2026-11-05" → "11월 5일" */
export function formatBusinessDate(date: string) {
  const [, month, day] = date.split('-').map(Number)
  return `${month}월 ${day}일`
}
