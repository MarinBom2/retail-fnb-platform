import { apiRequest } from '../../core/apiClient'
import type { CodeLabel } from '../../types/common'

export type { CodeLabel }

/** 서버 MemberResponse */
export interface MemberProfile {
  memberId: number
  email: string
  roleType: CodeLabel
}

export const memberApi = {
  /** 세션 확인에도 쓴다. 만료 안내를 띄우면 안 되면 auth: false */
  me: (options: { auth?: boolean } = {}) => apiRequest<MemberProfile>('/api/v1/me', options),
}
