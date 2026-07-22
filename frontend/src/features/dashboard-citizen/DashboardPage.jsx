import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import SeverityPill from '../../components/SeverityPill'
import StatusPill from '../../components/StatusPill'
import StatCard from '../../components/StatCard'
import { assetUrl } from '../../api/assetUrl'
import { fetchCitizenSummary } from '../../api/analyticsApi'
import { useAuth } from '../../auth/AuthContext'

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

function safetyScoreTone(score) {
  if (score >= 80) return 'secondary'
  if (score >= 50) return 'accent'
  return 'danger'
}

export default function DashboardPage() {
  const { user } = useAuth()
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    fetchCitizenSummary()
      .then((data) => {
        if (!cancelled) setSummary(data)
      })
      .catch((err) => {
        if (!cancelled) setError(err.friendlyMessage || 'Failed to load your dashboard.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div>
      <div className="mb-4">
        <h1 className="h4 fw-bold mb-1">Welcome back, {user?.fullName?.split(' ')[0]}</h1>
        <p className="text-muted-app mb-0">Here&apos;s the current state of your road reports.</p>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="row g-3 mb-4">
        <div className="col-6 col-lg-3">
          <StatCard label="Total Reports" value={loading ? '—' : summary.totalReports} accent="primary" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="Pending Repairs" value={loading ? '—' : summary.pendingRepairs} accent="accent" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="Resolved Reports" value={loading ? '—' : summary.resolvedReports} accent="secondary" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="High Severity" value={loading ? '—' : summary.highSeverityReports} accent="danger" />
        </div>
      </div>

      <div className="row g-3">
        <div className="col-lg-4">
          <div className="card p-4 h-100">
            <div className="detail-tile__label mb-2">Road Safety Score</div>
            {loading ? (
              <div className="text-muted-app small">Loading…</div>
            ) : (
              <>
                <div className={`safety-score safety-score--${safetyScoreTone(summary.roadSafetyScore)}`}>
                  {summary.roadSafetyScore}
                </div>
                <p className="small text-muted-app mb-0">
                  Based on the severity of your currently unresolved reports. Higher is safer.
                </p>
              </>
            )}
          </div>
        </div>

        <div className="col-lg-8">
          <div className="card p-4 h-100">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h2 className="h6 fw-semibold mb-0">Recent Activity</h2>
              <Link to="/my-reports" className="small">
                View all
              </Link>
            </div>

            {loading && <div className="text-muted-app small">Loading…</div>}

            {!loading && summary.recentActivity.length === 0 && (
              <div className="empty-state py-3">
                <p className="mb-3 small">You haven&apos;t reported any road damage yet.</p>
                <Link to="/report-damage" className="btn btn-app-primary btn-sm">
                  Report damage
                </Link>
              </div>
            )}

            {!loading && summary.recentActivity.length > 0 && (
              <ul className="activity-list mb-0">
                {summary.recentActivity.map((report) => (
                  <li key={report.id}>
                    <Link to={`/my-reports/${report.id}`} className="activity-list__item">
                      <img src={assetUrl(report.imageUrl)} alt="" className="activity-list__thumb" />
                      <div className="flex-grow-1 min-w-0">
                        <div className="fw-semibold text-truncate">
                          {report.addressText || 'Location not specified'}
                        </div>
                        <div className="small text-muted-app">{formatDate(report.createdAt)}</div>
                      </div>
                      <div className="d-flex flex-column align-items-end gap-1">
                        <SeverityPill severity={report.severity} />
                        <StatusPill status={report.status} />
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
