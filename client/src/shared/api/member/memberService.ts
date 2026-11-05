import { apiRequest } from '../../core/apiClient'

/** RoleType 같은 서버 enum 은 {code, description} 객체로 온다. 비교는 code, 표시는 description */
export interface CodeLabel {
  code: string
  description: string
}

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
