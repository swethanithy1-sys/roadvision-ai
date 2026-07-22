import { useAuth } from '../auth/AuthContext'
import { useThemeToggle } from './useThemeToggle'

export default function Topbar({ onMenuClick }) {
  const { user, logout } = useAuth()
  const { theme, toggleTheme } = useThemeToggle()

  const initials = (user?.fullName || '?')
    .split(' ')
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()

  return (
    <header className="app-topbar">
      <button className="app-topbar__menu-btn d-lg-none" onClick={onMenuClick} aria-label="Open menu">
        ☰
      </button>

      <div className="app-topbar__spacer" />

      <button className="app-topbar__icon-btn" onClick={toggleTheme} aria-label="Toggle theme">
        {theme === 'dark' ? '☀️' : '🌙'}
      </button>

      <div className="app-topbar__user">
        <div className="app-topbar__avatar">{initials}</div>
        <div className="d-none d-md-block">
          <div className="small fw-semibold">{user?.fullName}</div>
          <div className="small text-muted-app">{user?.role}</div>
        </div>
        <button className="btn btn-sm btn-outline-secondary ms-2" onClick={logout}>
          Log out
        </button>
      </div>
    </header>
  )
}
