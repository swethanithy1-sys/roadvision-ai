import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import AlertBanner from '../../components/AlertBanner'
import FormField from '../../components/FormField'
import { resetPassword } from '../../api/authApi'

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const navigate = useNavigate()

  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')

    if (password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    setIsLoading(true)
    try {
      await resetPassword(token, password)
      navigate('/login', { replace: true, state: { resetSuccess: true } })
    } catch (err) {
      setError(err.friendlyMessage || 'This reset link is invalid or has expired.')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="auth-layout">
      <aside className="auth-brand">
        <div className="auth-brand__content">
          <div className="auth-brand__mark">RV</div>
          <h2 className="fw-bold mb-3" style={{ fontSize: '1.9rem' }}>
            Choose a new password.
          </h2>
          <p className="mb-0" style={{ color: 'rgba(255,255,255,0.85)', lineHeight: 1.6 }}>
            Make it something you haven't used before.
          </p>
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
              <h1 className="h4 fw-bold mb-1">Reset your password</h1>
              <p className="text-muted-app small mb-0">Enter a new password for your account</p>
            </div>

            {!token ? (
              <AlertBanner>This reset link is missing its token.</AlertBanner>
            ) : (
              <>
                <AlertBanner>{error}</AlertBanner>

                <form onSubmit={handleSubmit} noValidate>
                  <FormField label="New password" htmlFor="password">
                    <input
                      id="password"
                      name="password"
                      type="password"
                      className="form-control"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required
                      minLength={8}
                      autoComplete="new-password"
                    />
                  </FormField>

                  <FormField label="Confirm new password" htmlFor="confirmPassword">
                    <input
                      id="confirmPassword"
                      name="confirmPassword"
                      type="password"
                      className="form-control"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      required
                      minLength={8}
                      autoComplete="new-password"
                    />
                  </FormField>

                  <button type="submit" className="btn btn-app-primary w-100 py-2 mt-2" disabled={isLoading}>
                    {isLoading ? 'Resetting…' : 'Reset password'}
                  </button>
                </form>
              </>
            )}

            <p className="text-center small text-muted-app mt-4 mb-0">
              <Link to="/login">Back to sign in</Link>
            </p>
          </div>
        </div>
      </main>
    </div>
  )
}
