import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import SeverityPill from '../../components/SeverityPill'
import StatCard from '../../components/StatCard'
import StatusPill from '../../components/StatusPill'
import { assetUrl } from '../../api/assetUrl'
import { fetchAdminSummary } from '../../api/analyticsApi'

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function AdminDashboardPage() {
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    fetchAdminSummary()
      .then((data) => {
        if (!cancelled) setSummary(data)
      })
      .catch((err) => {
        if (!cancelled) setError(err.friendlyMessage || 'Failed to load the admin dashboard.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  if (loading) {
    return <div className="card p-4 text-center text-muted-app">Loading admin dashboard…</div>
  }

  if (error || !summary) {
    return <div className="alert alert-danger">{error || 'No data available.'}</div>
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
          <h1 className="h4 fw-bold mb-1">Admin Dashboard</h1>
          <p className="text-muted-app mb-0">City-wide overview of road damage reports and repairs.</p>
        </div>
        <Link to="/admin/repairs" className="btn btn-app-primary">
          Manage repairs
        </Link>
      </div>

      <div className="row g-3 mb-4">
        <div className="col-6 col-lg-3">
          <StatCard label="Total Reports" value={summary.totalReports} accent="primary" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="Pending Reports" value={summary.pendingReports} accent="accent" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="High Priority Repairs" value={summary.highPriorityRepairs} accent="danger" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="Total Est. Repair Cost" value={`₹${summary.totalEstimatedCost}`} accent="secondary" />
        </div>
      </div>

      <div className="card p-4">
        <div className="d-flex justify-content-between align-items-center mb-3">
          <h2 className="h6 fw-semibold mb-0">Recent Reports</h2>
          <Link to="/admin/repairs" className="small">
            View all
          </Link>
        </div>

        {summary.recentReports.length === 0 ? (
          <div className="empty-state py-3 small">No reports have been submitted yet.</div>
        ) : (
          <ul className="activity-list mb-0">
            {summary.recentReports.map((report) => (
              <li key={report.id}>
                <Link to={`/admin/repairs?reportId=${report.id}`} className="activity-list__item">
                  <img src={assetUrl(report.imageUrl)} alt="" className="activity-list__thumb" />
                  <div className="flex-grow-1 min-w-0">
                    <div className="fw-semibold text-truncate">{report.addressText || 'Location not specified'}</div>
                    <div className="small text-muted-app">
                      {report.reporterName} · {formatDate(report.createdAt)}
                    </div>
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
  )
}
