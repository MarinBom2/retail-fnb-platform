import { apiRequest } from '../../core/apiClient'
import type { MemberProfile } from '../member/memberService'

/** 서버 MemberAuthenticationAPIFilter 가 읽는 키 그대로 (POST /api/v1/auth/login) */
export interface MemberLoginRequest {
  email: string
  password: string
}

/** 서버 MemberSignupRequest (FR-AU-001) */
export interface MemberSignupRequest {
  email: string
  password: string
}

export const authApi = {
  /** 성공하면 인증 쿠키(HttpOnly)가 붙고 본문은 회원 정보. 실패는 ApiError.code === 'LOGIN_FAILED' */
  login: (request: MemberLoginRequest) =>
    apiRequest<MemberProfile>('/api/v1/auth/login', { method: 'POST', auth: false, body: JSON.stringify(request) }),
  /** 가입만 한다. 로그인은 따로 부른다. 같은 이메일이면 ALREADY_EXIST */
  signup: (request: MemberSignupRequest) =>
    apiRequest<MemberProfile>('/api/v1/auth/signup', { method: 'POST', auth: false, body: JSON.stringify(request) }),
  /** 이 계정으로 발급한 토큰을 전부 무효로 만들고 쿠키를 지운다 (204) */
  logout: () => apiRequest<void>('/api/v1/auth/logout', { method: 'POST', auth: false }),
}
