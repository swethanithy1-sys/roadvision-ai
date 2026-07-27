import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import AlertBanner from '../../components/AlertBanner'
import FormField from '../../components/FormField'
import { useAuth } from '../../auth/AuthContext'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const from = location.state?.from?.pathname
  const resetSuccess = location.state?.resetSuccess

  function handleChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      const loggedInUser = await login(form.email, form.password)
      navigate(from || (loggedInUser.role === 'ADMIN' ? '/admin' : '/dashboard'), { replace: true })
    } catch (err) {
      setError(err.friendlyMessage || 'Unable to log in. Please check your credentials.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="auth-layout">
      <aside className="auth-brand">
        <div className="auth-brand__content">
          <div className="auth-brand__mark">RV</div>
          <h2 className="fw-bold mb-3" style={{ fontSize: '1.9rem' }}>
            Smarter roads, safer cities.
          </h2>
          <p className="mb-0" style={{ color: 'rgba(255,255,255,0.85)', lineHeight: 1.6 }}>
            AI-assisted road damage detection, reporting, and repair management — all in one
            place.
          </p>
          <ul className="auth-brand__points">
            <li>
              <span>📸</span> Report hazards with a single photo
            </li>
            <li>
              <span>🤖</span> Automatic severity &amp; cost estimation
            </li>
            <li>
              <span>🗺️</span> Live hazard map &amp; analytics
            </li>
          </ul>
        </div>
        <div style={{ position: 'relative', zIndex: 1, color: 'rgba(255,255,255,0.6)', fontSize: '0.8rem' }}>
          © RoadVision AI
        </div>
      </aside>

      <main className="auth-panel">
        <div className="auth-card">
          <div className="card p-4 p-md-5">
            <div className="text-center mb-4">
              <div className="auth-logo mx-auto mb-3 d-lg-none">RV</div>
              <h1 className="h4 fw-bold mb-1">Welcome back</h1>
              <p className="text-muted-app small mb-0">Sign in to your RoadVision AI account</p>
            </div>

            {resetSuccess && !error && (
              <div className="alert alert-success py-2 small mb-3">
                Password reset successfully. You can now sign in.
              </div>
            )}

            <AlertBanner>{error}</AlertBanner>

            <form onSubmit={handleSubmit} noValidate>
              <FormField label="Email address" htmlFor="email">
                <input
                  id="email"
                  name="email"
                  type="email"
                  className="form-control"
                  placeholder="you@example.com"
                  value={form.email}
                  onChange={handleChange}
                  required
                  autoComplete="email"
                />
              </FormField>

              <div className="mb-3">
                <label htmlFor="password" className="form-label fw-semibold">
                  Password
                </label>
                <input
                  id="password"
                  name="password"
                  type="password"
                  className="form-control"
                  placeholder="••••••••"
                  value={form.password}
                  onChange={handleChange}
                  required
                  autoComplete="current-password"
                />
              </div>

              <button type="submit" className="btn btn-app-primary w-100 py-2 mt-2" disabled={isSubmitting}>
                {isSubmitting ? 'Signing in…' : 'Sign in'}
              </button>
            </form>
          </div>
        </div>
      </main>
    </div>
  )
}
