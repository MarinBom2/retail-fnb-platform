import { Suspense, lazy } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import LandingPage from './pages/landing/LandingPage'
import { ROUTES } from './routes'
import { AuthProvider } from './shared/auth/AuthContext'
import { ConfirmProvider } from './shared/ui/ConfirmProvider'
import { ToastProvider } from './shared/ui/ToastProvider'

// 첫 화면 밖의 화면은 따로 받는다
const LoginPage = lazy(() => import('./pages/login/LoginPage'))
const StoreSelectPage = lazy(() => import('./pages/stores/StoreSelectPage'))
const AppShell = lazy(() => import('./shared/ui/layout/AppShell'))
const DashboardPage = lazy(() => import('./pages/dashboard/DashboardPage'))
const PosPage = lazy(() => import('./pages/pos/PosPage'))
const OrdersPage = lazy(() => import('./pages/orders/OrdersPage'))
const InventoryPage = lazy(() => import('./pages/inventory/InventoryPage'))
const TransfersPage = lazy(() => import('./pages/transfers/TransfersPage'))
const CatalogPage = lazy(() => import('./pages/catalog/CatalogPage'))
const BrandListPage = lazy(() => import('./pages/hq/BrandListPage'))
const HqShell = lazy(() => import('./shared/ui/layout/HqShell'))
const BrandStoresPage = lazy(() => import('./pages/hq/BrandStoresPage'))
const BrandProductsPage = lazy(() => import('./pages/hq/BrandProductsPage'))

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <ConfirmProvider>
            <Suspense fallback={<div className="page-loading" aria-busy="true" />}>
              <Routes>
                <Route path={ROUTES.SCR01_LANDING} element={<LandingPage />} />
                <Route path={ROUTES.SCR02_LOGIN} element={<LoginPage />} />
                <Route path={ROUTES.SCR03_STORES} element={<StoreSelectPage />} />
                <Route path={ROUTES.SCR04_DASHBOARD} element={<AppShell />}>
                  <Route index element={<DashboardPage />} />
                  <Route path="pos" element={<PosPage />} />
                  <Route path="orders" element={<OrdersPage />} />
                  <Route path="inventory" element={<InventoryPage />} />
                  <Route path="transfers" element={<TransfersPage />} />
                  <Route path="catalog" element={<CatalogPage />} />
                </Route>
                <Route path={ROUTES.SCR10_BRANDS} element={<BrandListPage />} />
                <Route path="/brands/:brandId" element={<HqShell />}>
                  <Route index element={<Navigate to="stores" replace />} />
                  <Route path="stores" element={<BrandStoresPage />} />
                  <Route path="products" element={<BrandProductsPage />} />
                </Route>
                <Route path="*" element={<LandingPage />} />
              </Routes>
            </Suspense>
          </ConfirmProvider>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
