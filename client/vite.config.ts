import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import babel from '@rolldown/plugin-babel'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    babel({ presets: [reactCompilerPreset()] })
  ],
  // 로컬 개발: 화면은 여기(9191, HMR), API 는 서버(9190)로 프록시한다.
  // 브라우저에는 모든 요청이 9191 한 오리진으로 보여서 운영(같은 jar 가 화면·API 서빙)과 조건이 같다.
  // 그래서 로컬에도 CORS 설정이 필요 없고, HttpOnly 인증 쿠키도 운영과 같이 동작한다 (md/runtime-structure.md).
  server: {
    port: 9191,
    strictPort: true,
    proxy: {
      // changeOrigin 을 켜지 않는다: Host 가 localhost:9191 로 남아 서버가 만드는 주소도 9191 을 가리킨다
      '/api': 'http://localhost:9190',
    },
  },
})
