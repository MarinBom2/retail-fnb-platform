/**
 * 서버 오류 응답. RFC 9457 Problem Details (Content-Type: application/problem+json).
 * 서버 ProblemDetails·ErrorCode 와 1:1 (md/develop-principal.md §7).
 * 성공 응답은 봉투 없이 DTO 그대로 온다.
 */
export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  /** 화면에 그대로 보여 줄 문장 */
  detail?: string
  instance?: string
  /** 서버 ErrorCode 이름. 분기는 문구가 아니라 이 값으로 한다 (예: 'INVALID_ACCESS_TOKEN') */
  code?: string
  /** 입력값 검증 실패(INVALID_REQUEST_DATA) 때 필드별 오류 */
  errors?: FieldError[]
  /** 그 밖의 확장 필드 */
  [key: string]: unknown
}

export interface FieldError {
  field: string
  message: string
}

/** 서버 PageResponse 와 1:1. pageNumber 는 1부터 */
export interface PageResponse<TItem> {
  content: TItem[];
  pageNumber: number;
  pageSize: number;
  totalSize: number;
  totalPageNumber: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}

/** 서버 enum 은 {code, description} 객체로 온다. 비교는 code, 표시는 description */
export interface CodeLabel {
  code: string
  description: string
}
