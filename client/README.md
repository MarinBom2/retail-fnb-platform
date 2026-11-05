# client

Vite + React 19 + TypeScript 5.9 SPA. 실행·빌드 구조는 [`md/runtime-structure.md`](../md/runtime-structure.md).

```bash
npm install
npm run dev      # http://localhost:9191 (/api 는 9190 서버로 프록시)
npm run build    # tsc 타입 검사 후 dist/
npm run lint
```

## 폴더

| 폴더 | 내용 |
|---|---|
| `src/pages/<화면>` | 화면 하나당 폴더 하나. 컴포넌트 이름은 `XxxPage` |
| `src/shared/core` | `apiClient.ts`: 모든 API 호출의 공통 수신부 (ProblemDetail → `ApiError`, 401 처리) |
| `src/shared/api/<도메인>` | 서버 API 호출과 요청·응답 타입 (서버 DTO 와 1:1) |
| `src/shared/auth` | 로그인 상태 (`AuthProvider`, `useAuth`). 토큰은 HttpOnly 쿠키라 화면이 만지지 않는다 |
| `src/shared/types` | 서버 공통 응답 타입 (`ProblemDetail`) |
| `src/routes.ts` | 화면 경로. 키에 화면 ID(`SCR01_HOME`)를 쓴다 |
