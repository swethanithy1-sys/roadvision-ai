import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth/AuthContext'
import ProtectedRoute from './auth/ProtectedRoute'
import RoleRoute from './auth/RoleRoute'
import AppShell from './layouts/AppShell'
import LoginPage from './features/auth/LoginPage'
import RegisterPage from './features/auth/RegisterPage'
import DashboardPage from './features/dashboard-citizen/DashboardPage'
import ReportDamagePage from './features/report-damage/ReportDamagePage'
import MyReportsPage from './features/my-reports/MyReportsPage'
import ReportDetailsPage from './features/report-details/ReportDetailsPage'
import AnalyticsPage from './features/analytics/AnalyticsPage'
import HazardMapPage from './features/hazard-map/HazardMapPage'
import AdminDashboardPage from './features/dashboard-admin/AdminDashboardPage'
import RepairManagementPage from './features/repair-management/RepairManagementPage'
import WorkOrderPage from './features/work-order/WorkOrderPage'

function homePathFor(user) {
  return user?.role === 'ADMIN' ? '/admin' : '/dashboard'
}

function PublicOnlyRoute({ children }) {
  const { isAuthenticated, user } = useAuth()
  return isAuthenticated ? <Navigate to={homePathFor(user)} replace /> : children
}

function RoleHomeRedirect() {
  const { user } = useAuth()
  return <Navigate to={homePathFor(user)} replace />
}

function AppRoutes() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <PublicOnlyRoute>
            <LoginPage />
          </PublicOnlyRoute>
        }
      />
      <Route
        path="/register"
        element={
          <PublicOnlyRoute>
            <RegisterPage />
          </PublicOnlyRoute>
        }
      />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/report-damage" element={<ReportDamagePage />} />
          <Route path="/my-reports" element={<MyReportsPage />} />
          <Route path="/my-reports/:id" element={<ReportDetailsPage />} />
          <Route path="/analytics" element={<AnalyticsPage />} />
          <Route path="/hazard-map" element={<HazardMapPage />} />

          <Route element={<RoleRoute allow={['ADMIN']} />}>
            <Route path="/admin" element={<AdminDashboardPage />} />
            <Route path="/admin/repairs" element={<RepairManagementPage />} />
            <Route path="/admin/reports/:id/work-order" element={<WorkOrderPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="/" element={<RoleHomeRedirect />} />
      <Route path="*" element={<RoleHomeRedirect />} />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}
