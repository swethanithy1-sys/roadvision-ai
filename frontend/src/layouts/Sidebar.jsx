import { NavLink } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ADMIN_NAV, CITIZEN_NAV } from './navConfig'

export default function Sidebar({ open, onClose }) {
  const { isAdmin } = useAuth()
  const items = isAdmin ? ADMIN_NAV : CITIZEN_NAV

  return (
    <>
      <aside className={`app-sidebar ${open ? 'is-open' : ''}`}>
        <div className="app-sidebar__brand">
          <span className="auth-logo">RV</span>
          <div>
            <div className="fw-bold">RoadVision AI</div>
            <div className="small text-muted-app">Smart Road Monitoring</div>
          </div>
        </div>

        <nav className="app-sidebar__nav">
          {items.map((item) =>
            item.available ? (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) => `app-sidebar__link ${isActive ? 'is-active' : ''}`}
                onClick={onClose}
              >
                {item.label}
              </NavLink>
            ) : (
              <span key={item.path} className="app-sidebar__link is-disabled" title="Coming soon">
                {item.label}
                <span className="badge-soon">Soon</span>
              </span>
            )
          )}
        </nav>
      </aside>
      {open && <div className="app-sidebar__backdrop" onClick={onClose} />}
    </>
  )
}
