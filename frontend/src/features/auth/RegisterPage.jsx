import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AlertBanner from '../../components/AlertBanner'
import FormField from '../../components/FormField'
import { useAuth } from '../../auth/AuthContext'
import { verifyOtp } from '../../api/authApi'

const INITIAL_DETAILS = { fullName: '', email: '', phone: '' }

export default function RegisterPage() {
  const { register, completeRegistration } = useAuth()
  const navigate = useNavigate()

  const [step, setStep] = useState('details') // details | otp | password
  const [details, setDetails] = useState(INITIAL_DETAILS)
  const [otp, setOtp] = useState('')
  const [accessToken, setAccessToken] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  function handleDetailsChange(e) {
    setDetails((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  async function handleSendCode(e) {
    e.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      await register(details)
      setStep('otp')
    } catch (err) {
      setError(err.friendlyMessage || 'Unable to create your account.')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleVerifyOtp(e) {
    e.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      const result = await verifyOtp({ email: details.email, otp })
      setAccessToken(result.accessToken)
      setStep('password')
    } catch (err) {
      setError(err.friendlyMessage || 'Invalid or expired code.')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleSetPassword(e) {
    e.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      const user = await completeRegistration({ accessToken, password })
      navigate(user.role === 'ADMIN' ? '/admin' : '/dashboard', { replace: true })
    } catch (err) {
      setError(err.friendlyMessage || 'Unable to set your password.')
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
            {step === 'details' && (
              <>
                <div className="text-center mb-4">
                  <div className="auth-logo mx-auto mb-3 d-lg-none">RV</div>
                  <h1 className="h4 fw-bold mb-1">Create your account</h1>
                  <p className="text-muted-app small mb-0">Start managing road reports in minutes</p>
                </div>

                <AlertBanner>{error}</AlertBanner>

                <form onSubmit={handleSendCode} noValidate>
                  <FormField label="Full name" htmlFor="fullName">
                    <input
                      id="fullName"
                      name="fullName"
                      className="form-control"
                      value={details.fullName}
                      onChange={handleDetailsChange}
                      required
                    />
                  </FormField>

                  <FormField label="Email address" htmlFor="email">
                    <input
                      id="email"
                      name="email"
                      type="email"
                      className="form-control"
                      value={details.email}
                      onChange={handleDetailsChange}
                      required
                      autoComplete="email"
                    />
                  </FormField>

                  <FormField label="Phone (optional)" htmlFor="phone">
                    <input
                      id="phone"
                      name="phone"
                      className="form-control"
                      value={details.phone}
                      onChange={handleDetailsChange}
                      autoComplete="tel"
                    />
                  </FormField>

                  <button type="submit" className="btn btn-app-primary w-100 py-2 mt-2" disabled={isSubmitting}>
                    {isSubmitting ? 'Sending code…' : 'Send verification code'}
                  </button>
                </form>

                <p className="text-center small text-muted-app mt-4 mb-0">
                  Already have an account? <Link to="/login">Sign in</Link>
                </p>
              </>
            )}

            {step === 'otp' && (
              <>
                <div className="text-center mb-4">
                  <div className="auth-logo mx-auto mb-3 d-lg-none">RV</div>
                  <h1 className="h4 fw-bold mb-1">Verify your email</h1>
                  <p className="text-muted-app small mb-0">
                    Enter the code we sent to <strong>{details.email}</strong>
                  </p>
                </div>

                <AlertBanner>{error}</AlertBanner>

                <form onSubmit={handleVerifyOtp} noValidate>
                  <FormField label="Verification code" htmlFor="otp">
                    <input
                      id="otp"
                      name="otp"
                      className="form-control text-center"
                      value={otp}
                      onChange={(e) => setOtp(e.target.value)}
                      required
                      inputMode="numeric"
                      autoComplete="one-time-code"
                      autoFocus
                    />
                  </FormField>

                  <button type="submit" className="btn btn-app-primary w-100 py-2 mt-2" disabled={isSubmitting}>
                    {isSubmitting ? 'Verifying…' : 'Verify'}
                  </button>
                </form>

                <p className="text-center small text-muted-app mt-4 mb-0">
                  Wrong email?{' '}
                  <button type="button" className="btn btn-link btn-sm p-0" onClick={() => setStep('details')}>
                    Start over
                  </button>
                </p>
              </>
            )}

            {step === 'password' && (
              <>
                <div className="text-center mb-4">
                  <div className="auth-logo mx-auto mb-3 d-lg-none">RV</div>
                  <h1 className="h4 fw-bold mb-1">Set your password</h1>
                  <p className="text-muted-app small mb-0">Email verified — last step</p>
                </div>

                <AlertBanner>{error}</AlertBanner>

                <form onSubmit={handleSetPassword} noValidate>
                  <FormField label="Password" htmlFor="password">
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
                      autoFocus
                    />
                  </FormField>

                  <button type="submit" className="btn btn-app-primary w-100 py-2 mt-2" disabled={isSubmitting}>
                    {isSubmitting ? 'Creating account…' : 'Create account'}
                  </button>
                </form>
              </>
            )}
          </div>
        </div>
      </main>
    </div>
  )
}
