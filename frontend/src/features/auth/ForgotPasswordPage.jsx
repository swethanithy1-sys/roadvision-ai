import { useState } from 'react'
import { Link } from 'react-router-dom'
import AlertBanner from '../../components/AlertBanner'
import FormField from '../../components/FormField'
import { forgotPassword } from '../../api/authApi'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [submitted, setSubmitted] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setIsLoading(true)
    try {
      await forgotPassword(email)
      setSubmitted(true)
    } catch (err) {
      setError(err.friendlyMessage || 'Something went wrong. Please try again.')
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
            Forgot your password?
          </h2>
          <p className="mb-0" style={{ color: 'rgba(255,255,255,0.85)', lineHeight: 1.6 }}>
            No problem — we'll email you a link to choose a new one.
          </p>
        </div>
        <div style={{ position: 'relative', zIndex: 1, color: 'rgba(255,255,255,0.6)', fontSize: '0.8rem' }}>
          © RoadVision AI
        </div>
      </aside>

      <main className="auth-panel">
        <div className="auth-card">
          <div className="card p-4 p-md-5">
            {submitted ? (
              <div className="text-center">
                <div className="auth-logo mx-auto mb-3 d-lg-none">RV</div>
                <h1 className="h4 fw-bold mb-2">Check your email</h1>
                <p className="text-muted-app small mb-0">
                  If <strong>{email}</strong> is registered, we've sent a password reset link to it.
                </p>
                <p className="text-center small text-muted-app mt-4 mb-0">
                  <Link to="/login">Back to sign in</Link>
                </p>
              </div>
            ) : (
              <>
                <div className="text-center mb-4">
                  <div className="auth-logo mx-auto mb-3 d-lg-none">RV</div>
                  <h1 className="h4 fw-bold mb-1">Reset your password</h1>
                  <p className="text-muted-app small mb-0">Enter your account email address</p>
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
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      required
                      autoComplete="email"
                    />
                  </FormField>

                  <button type="submit" className="btn btn-app-primary w-100 py-2 mt-2" disabled={isLoading}>
                    {isLoading ? 'Sending…' : 'Send reset link'}
                  </button>
                </form>

                <p className="text-center small text-muted-app mt-4 mb-0">
                  <Link to="/login">Back to sign in</Link>
                </p>
              </>
            )}
          </div>
        </div>
      </main>
    </div>
  )
}
