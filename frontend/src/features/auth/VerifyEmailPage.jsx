import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const { verifyEmail } = useAuth()
  const navigate = useNavigate()

  const [status, setStatus] = useState('verifying') // verifying | success | error
  const [error, setError] = useState('')
  const hasRun = useRef(false)

  useEffect(() => {
    if (hasRun.current) return
    hasRun.current = true

    if (!token) {
      setStatus('error')
      setError('This verification link is missing its token.')
      return
    }

    verifyEmail(token)
      .then((user) => {
        setStatus('success')
        setTimeout(() => {
          navigate(user.role === 'ADMIN' ? '/admin' : '/dashboard', { replace: true })
        }, 1500)
      })
      .catch((err) => {
        setStatus('error')
        setError(err.friendlyMessage || 'This verification link is invalid or has expired.')
      })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  return (
    <div className="auth-layout">
      <aside className="auth-brand">
        <div className="auth-brand__content">
          <div className="auth-brand__mark">RV</div>
          <h2 className="fw-bold mb-3" style={{ fontSize: '1.9rem' }}>
            Almost there.
          </h2>
          <p className="mb-0" style={{ color: 'rgba(255,255,255,0.85)', lineHeight: 1.6 }}>
            Confirming your email so you can start reporting road damage.
          </p>
        </div>
        <div style={{ position: 'relative', zIndex: 1, color: 'rgba(255,255,255,0.6)', fontSize: '0.8rem' }}>
          © RoadVision AI
        </div>
      </aside>

      <main className="auth-panel">
        <div className="auth-card">
          <div className="card p-4 p-md-5 text-center">
            <div className="auth-logo mx-auto mb-3 d-lg-none">RV</div>

            {status === 'verifying' && (
              <>
                <h1 className="h4 fw-bold mb-2">Verifying your email…</h1>
                <p className="text-muted-app small mb-0">Just a moment.</p>
              </>
            )}

            {status === 'success' && (
              <>
                <h1 className="h4 fw-bold mb-2">Email verified!</h1>
                <p className="text-muted-app small mb-0">Taking you to your dashboard…</p>
              </>
            )}

            {status === 'error' && (
              <>
                <h1 className="h4 fw-bold mb-2">Verification failed</h1>
                <p className="text-muted-app small mb-4">{error}</p>
                <Link to="/login" className="btn btn-app-primary w-100 py-2">
                  Back to sign in
                </Link>
              </>
            )}
          </div>
        </div>
      </main>
    </div>
  )
}
