import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import AlertBanner from '../../components/AlertBanner'
import FormField from '../../components/FormField'
import { useAuth } from '../../auth/AuthContext'

export default function LoginPage() {
  const { login, isLoading } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')

  const from = location.state?.from?.pathname || '/dashboard'

  function handleChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    try {
      await login(form.email, form.password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(err.friendlyMessage || 'Unable to log in. Please check your credentials.')
    }
  }

  return (
    <div className="auth-page d-flex align-items-center justify-content-center min-vh-100">
      <div className="card p-4 p-md-5" style={{ width: '100%', maxWidth: 420 }}>
        <div className="text-center mb-4">
          <div className="auth-logo mx-auto mb-3">RV</div>
          <h1 className="h4 fw-bold mb-1">Welcome back</h1>
          <p className="text-muted-app small mb-0">Sign in to RoadVision AI</p>
        </div>

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

          <FormField label="Password" htmlFor="password">
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
          </FormField>

          <button type="submit" className="btn btn-app-primary w-100 py-2 mt-2" disabled={isLoading}>
            {isLoading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <p className="text-center small text-muted-app mt-4 mb-0">
          Don&apos;t have an account? <Link to="/register">Create one</Link>
        </p>

        <div className="demo-hint text-center small text-muted-app mt-4">
          Demo: admin@roadvision.ai / citizen1@roadvision.ai — password <code>Password123!</code>
        </div>
      </div>
    </div>
  )
}
