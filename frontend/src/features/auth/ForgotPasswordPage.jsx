import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AlertBanner from '../../components/AlertBanner'
import FormField from '../../components/FormField'
import { forgotPassword, resetPassword } from '../../api/authApi'

export default function ForgotPasswordPage() {
  const navigate = useNavigate()

  const [step, setStep] = useState('email') // email | reset
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSendCode(e) {
    e.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      await forgotPassword(email)
      setStep('reset')
    } catch (err) {
      setError(err.friendlyMessage || 'Something went wrong. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleReset(e) {
    e.preventDefault()
    setError('')

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    setIsSubmitting(true)
    try {
      await resetPassword({ email, otp, newPassword })
      navigate('/login', { replace: true, state: { resetSuccess: true } })
    } catch (err) {
      setError(err.friendlyMessage || 'Invalid or expired code.')
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
            Forgot your password?
          </h2>
          <p className="mb-0" style={{ color: 'rgba(255,255,255,0.85)', lineHeight: 1.6 }}>
            No problem — we'll email you a code to choose a new one.
          </p>
        </div>
        <div style={{ position: 'relative', zIndex: 1, color: 'rgba(255,255,255,0.6)', fontSize: '0.8rem' }}>
          © RoadVision AI
        </div>
      </aside>

      <main className="auth-panel">
        <div className="auth-card">
          <div className="card p-4 p-md-5">
            {step === 'email' ? (
              <>
                <div className="text-center mb-4">
                  <div className="auth-logo mx-auto mb-3 d-lg-none">RV</div>
                  <h1 className="h4 fw-bold mb-1">Reset your password</h1>
                  <p className="text-muted-app small mb-0">Enter your account email address</p>
                </div>

                <AlertBanner>{error}</AlertBanner>

                <form onSubmit={handleSendCode} noValidate>
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

                  <button type="submit" className="btn btn-app-primary w-100 py-2 mt-2" disabled={isSubmitting}>
                    {isSubmitting ? 'Sending…' : 'Send reset code'}
                  </button>
                </form>

                <p className="text-center small text-muted-app mt-4 mb-0">
                  <Link to="/login">Back to sign in</Link>
                </p>
              </>
            ) : (
              <>
                <div className="text-center mb-4">
                  <div className="auth-logo mx-auto mb-3 d-lg-none">RV</div>
                  <h1 className="h4 fw-bold mb-1">Enter your code</h1>
                  <p className="text-muted-app small mb-0">
                    If <strong>{email}</strong> is registered, we've sent it a reset code
                  </p>
                </div>

                <AlertBanner>{error}</AlertBanner>

                <form onSubmit={handleReset} noValidate>
                  <FormField label="Reset code" htmlFor="otp">
                    <input
                      id="otp"
                      name="otp"
                      className="form-control"
                      value={otp}
                      onChange={(e) => setOtp(e.target.value)}
                      required
                      inputMode="numeric"
                      autoComplete="one-time-code"
                    />
                  </FormField>

                  <FormField label="New password" htmlFor="newPassword">
                    <input
                      id="newPassword"
                      name="newPassword"
                      type="password"
                      className="form-control"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
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

                  <button type="submit" className="btn btn-app-primary w-100 py-2 mt-2" disabled={isSubmitting}>
                    {isSubmitting ? 'Resetting…' : 'Reset password'}
                  </button>
                </form>

                <p className="text-center small text-muted-app mt-4 mb-0">
                  <button type="button" className="btn btn-link btn-sm p-0" onClick={() => setStep('email')}>
                    Back
                  </button>
                </p>
              </>
            )}
          </div>
        </div>
      </main>
    </div>
  )
}
