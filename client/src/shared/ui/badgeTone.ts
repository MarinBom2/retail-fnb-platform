export type BadgeTone = 'neutral' | 'primary' | 'success' | 'warning' | 'danger' | 'info'

/** 업종마다 색을 고정한다 (화면 어디서나 같은 색) */
export function businessTypeTone(code: string): BadgeTone {
  switch (code) {
    case 'GENERAL_MERCHANDISE':
      return 'info'
    case 'APPAREL':
      return 'primary'
    case 'FOOD_SERVICE':
      return 'warning'
    default:
      return 'neutral'
  }
}

export function orderStatusTone(code: string): BadgeTone {
  switch (code) {
    case 'OPEN':
      return 'info'
    case 'PAID':
      return 'success'
    case 'REFUNDED':
      return 'warning'
    default:
      return 'neutral'
  }
}

export function transferStatusTone(code: string): BadgeTone {
  switch (code) {
    case 'REQUESTED':
      return 'info'
    case 'APPROVED':
    case 'SHIPPED':
      return 'primary'
    case 'RECEIVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    default:
      return 'neutral'
  }
}
