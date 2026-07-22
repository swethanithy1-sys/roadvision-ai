import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './AuthContext'

export default function RoleRoute({ allow }) {
  const { user } = useAuth()

  if (!user || !allow.includes(user.role)) {
    return <Navigate to="/dashboard" replace />
  }

  return <Outlet />
}
