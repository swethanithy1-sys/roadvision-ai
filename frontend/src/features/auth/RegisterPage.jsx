import { useState } from 'react'
import { Link } from 'react-router-dom'
import AlertBanner from '../../components/AlertBanner'
import FormField from '../../components/FormField'
import { useAuth } from '../../auth/AuthContext'

const INITIAL_FORM = { fullName: '', email: '', phone: '', password: '' }

export default function RegisterPage() {
  const { register, isLoading } = useAuth()

  const [form, setForm] = useState(INITIAL_FORM)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [submittedEmail, setSubmittedEmail] = useState('')

  function handleChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setFieldErrors({})
    try {
      const result = await register(form)
      setSubmittedEmail(result.email)
    } catch (err) {
      const data = err.response?.data
      if (data?.data && typeof data.data === 'object') {
        setFieldErrors(data.data)
      }
      setError(err.friendlyMessage || 'Unable to create your account.')
    }
  }

  return (
    <div className="auth-layout">
      <aside className="auth-brand">
        <div className="auth-brand__content">
          <div className="auth-brand__mark">RV</div>
          <h2 className="fw-bold mb-3" style={{ fontSize: '1.9rem' }}>
            Join the road safety movement.
          </h2>
          <p className="mb-0" style={{ color: 'rgba(255,255,255,0.85)', lineHeight: 1.6 }}>
            Create a free account to report damage in your area and help authorities keep the
            roads safe.
          </p>
          <ul className="auth-brand__points">
            <li>
              <span>⚡</span> Instant AI damage analysis
            </li>
            <li>
              <span>📍</span> Pinpoint hazards with GPS
            </li>
            <li>
              <span>🔔</span> Track your reports to resolution
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
            {submittedEmail ? (
              <div className="text-center">
                <div className="auth-logo mx-auto mb-3 d-lg-none">RV</div>
                <h1 className="h4 fw-bold mb-2">Check your email</h1>
                <p className="text-muted-app small mb-0">
                  We sent a verification link to <strong>{submittedEmail}</strong>. Click it to
                  activate your account, then sign in.
                </p>
                <p className="text-center small text-muted-app mt-4 mb-0">
                  Already verified? <Link to="/login">Sign in</Link>
                </p>
              </div>
            ) : (
              <>
                <div className="text-center mb-4">
                  <div className="auth-logo mx-auto mb-3 d-lg-none">RV</div>
                  <h1 className="h4 fw-bold mb-1">Create your account</h1>
                  <p className="text-muted-app small mb-0">Report road damage and track repairs</p>
                </div>

                <AlertBanner>{error}</AlertBanner>

                <form onSubmit={handleSubmit} noValidate>
                  <FormField label="Full name" htmlFor="fullName" error={fieldErrors.fullName}>
                    <input
                      id="fullName"
                      name="fullName"
                      className="form-control"
                      value={form.fullName}
                      onChange={handleChange}
                      required
                    />
                  </FormField>

                  <FormField label="Email address" htmlFor="email" error={fieldErrors.email}>
                    <input
                      id="email"
                      name="email"
                      type="email"
                      className="form-control"
                      value={form.email}
                      onChange={handleChange}
                      required
                      autoComplete="email"
                    />
                  </FormField>

                  <FormField label="Phone (optional)" htmlFor="phone">
                    <input
                      id="phone"
                      name="phone"
                      className="form-control"
                      value={form.phone}
                      onChange={handleChange}
                      autoComplete="tel"
                    />
                  </FormField>

                  <FormField label="Password" htmlFor="password" error={fieldErrors.password}>
                    <input
                      id="password"
                      name="password"
                      type="password"
                      className="form-control"
                      value={form.password}
                      onChange={handleChange}
                      required
                      minLength={8}
                      autoComplete="new-password"
                    />
                  </FormField>

                  <button type="submit" className="btn btn-app-primary w-100 py-2 mt-2" disabled={isLoading}>
                    {isLoading ? 'Creating account…' : 'Create account'}
                  </button>
                </form>

                <p className="text-center small text-muted-app mt-4 mb-0">
                  Already have an account? <Link to="/login">Sign in</Link>
                </p>
              </>
            )}
          </div>
        </div>
      </main>
    </div>
  )
}
