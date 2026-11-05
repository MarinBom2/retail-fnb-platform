import { Suspense, lazy } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import HomePage from './pages/home/HomePage'
import { ROUTES } from './routes'
import { AuthProvider } from './shared/auth/AuthContext'

// 첫 화면 밖의 화면은 따로 받는다
const LoginPage = lazy(() => import('./pages/login/LoginPage'))

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Suspense fallback={<div className="page-loading" aria-busy="true" />}>
          <Routes>
            <Route path={ROUTES.SCR01_HOME} element={<HomePage />} />
            <Route path={ROUTES.SCR02_LOGIN} element={<LoginPage />} />
            <Route path="*" element={<HomePage />} />
          </Routes>
        </Suspense>
      </AuthProvider>
    </BrowserRouter>
  )
}
