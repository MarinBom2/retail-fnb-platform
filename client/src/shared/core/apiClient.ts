import type { FieldError, ProblemDetail } from '../types/common'

/**
 * 기본은 상대 경로(같은 오리진). 운영은 server jar 가 화면과 API 를 같은 오리진으로 서빙하고,
 * 로컬 개발은 vite dev 서버(9191)가 /api 를 서버(9190)로 프록시한다 (vite.config.ts).
 * .env.* 는 gitignore 라 CI 빌드에는 없다. 그래서 기본값이 운영에서도 맞는 값이어야 한다.
 * 다른 서버를 직접 부르고 싶을 때만 VITE_API_BASE_URL 에 절대 주소를 넣는다.
 */
const DEFAULT_API_BASE_URL = ''

const envApiBaseUrl = import.meta.env.VITE_API_BASE_URL

export const API_BASE_URL = String(envApiBaseUrl ?? DEFAULT_API_BASE_URL).replace(
  /\/$/,
  '',
)

type ApiRequestOptions = Omit<RequestInit, 'headers' | 'credentials'> & {
  /** 401 을 세션 만료로 볼지. 인증 전 요청은 false */
  auth?: boolean
  headers?: Record<string, string>
  query?: Record<string, string | number | boolean | null | undefined>
}

/**
 * 서버 오류 하나. message 는 화면에 보여 줄 문장(ProblemDetail.detail), 분기는 code 로 한다.
 * 서버가 ProblemDetail 을 주지 못한 경우(게이트웨이 오류 페이지 등)는 code 가 없고 message 는 상태별 기본 문장이다.
 */
export class ApiError extends Error {
  status: number
  code?: string
  fieldErrors: FieldError[]
  problem?: ProblemDetail

  constructor(message: string, status: number, problem?: ProblemDetail) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = problem?.code
    this.fieldErrors = problem?.errors ?? []
    this.problem = problem
  }
}

/** 세션이 끊겼을 때 AuthProvider 에 알리는 통로 (라우터 밖이라 직접 옮기지 않는다) */
type UnauthorizedHandler = () => void

let unauthorizedHandler: UnauthorizedHandler | null = null

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null) {
  unauthorizedHandler = handler
}

function buildUrl(path: string, query: ApiRequestOptions['query']) {
  // 기본 주소가 비면 현재 오리진 기준 상대 경로
  const url = new URL(`${API_BASE_URL}${path}`, window.location.origin)

  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value === null || value === undefined || value === '') continue
      url.searchParams.set(key, String(value))
    }
  }

  return url.toString()
}

/**
 * 모든 API 호출의 공통 수신부.
 * - 성공(2xx): 서버가 보낸 DTO 를 그대로 돌려준다. 본문이 없으면(204 등) undefined
 * - 실패: ProblemDetail 을 읽어 ApiError 로 던진다. 판단 기준은 HTTP 상태 하나다
 * - 401 이고 auth 가 true 면 세션 만료로 보고 AuthProvider 에 알린다
 * 인증은 HttpOnly 쿠키가 맡는다 (credentials: 'include')
 */
export async function apiRequest<TData>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<TData> {
  const { auth = true, headers, query, ...requestOptions } = options

  const response = await fetch(buildUrl(path, query), {
    ...requestOptions,
    credentials: 'include',
    headers: {
      Accept: 'application/json, application/problem+json',
      // FormData 는 브라우저가 boundary 를 붙여 Content-Type 을 직접 정한다 
      ...(requestOptions.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...headers,
    },
  })

  const body = await readJson(response)

  if (response.ok) {
    return body as TData
  }

  if (response.status === 401 && auth) {
    handleUnauthorized()
  }

  const problem = isProblemDetail(body) ? body : undefined
  throw new ApiError(problem?.detail || fallbackMessage(response.status), response.status, problem)
}

/** application/json, application/problem+json 둘 다 읽는다. 게이트웨이 오류 페이지처럼 JSON 이 아니면 undefined */
async function readJson(response: Response): Promise<unknown> {
  const contentType = response.headers.get('content-type') ?? ''

  if (response.status === 204 || !contentType.includes('json')) {
    return undefined
  }

  try {
    return await response.json()
  } catch {
    return undefined
  }
}

function isProblemDetail(body: unknown): body is ProblemDetail {
  return typeof body === 'object' && body !== null && typeof (body as ProblemDetail).status === 'number'
}

function fallbackMessage(httpStatus: number) {
  if (httpStatus === 401) return '로그인이 필요합니다.'
  if (httpStatus === 403) return '접근 권한이 없습니다.'
  if (httpStatus === 404) return '요청한 정보를 찾을 수 없습니다.'
  if (httpStatus >= 500) return '서버에서 오류가 발생했습니다.'
  return '요청을 처리하지 못했습니다.'
}

/** 쿠키는 HttpOnly 라 여기서 지울 수 없다. 앱에 알리기만 한다 */
function handleUnauthorized() {
  if (typeof window === 'undefined') return

  unauthorizedHandler?.()
}
