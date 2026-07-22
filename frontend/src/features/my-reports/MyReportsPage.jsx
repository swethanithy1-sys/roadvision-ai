import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import SeverityPill from '../../components/SeverityPill'
import StatusPill from '../../components/StatusPill'
import { assetUrl } from '../../api/assetUrl'
import { fetchMyReports } from '../../api/reportApi'

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function MyReportsPage() {
  const [state, setState] = useState({ loading: true, error: '', reports: [] })

  useEffect(() => {
    let cancelled = false

    fetchMyReports(0, 50)
      .then((data) => {
        if (!cancelled) setState({ loading: false, error: '', reports: data.content })
      })
      .catch((err) => {
        if (!cancelled) {
          setState({ loading: false, error: err.friendlyMessage || 'Failed to load your reports.', reports: [] })
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
          <h1 className="h4 fw-bold mb-1">My Reports</h1>
          <p className="text-muted-app mb-0">Track the status of the damage you&apos;ve reported.</p>
        </div>
        <Link to="/report-damage" className="btn btn-app-primary">
          + Report damage
        </Link>
      </div>

      {state.loading && <div className="card p-4 text-center text-muted-app">Loading your reports…</div>}

      {!state.loading && state.error && <div className="alert alert-danger">{state.error}</div>}

      {!state.loading && !state.error && state.reports.length === 0 && (
        <div className="card p-4">
          <div className="empty-state">
            <h2 className="h6 fw-semibold mb-2">No reports yet</h2>
            <p className="mb-3 small">You haven&apos;t reported any road damage. Submit your first report to get started.</p>
            <Link to="/report-damage" className="btn btn-app-primary">
              Report damage
            </Link>
          </div>
        </div>
      )}

      {!state.loading && state.reports.length > 0 && (
        <div className="row g-3">
          {state.reports.map((report) => (
            <div className="col-12 col-md-6 col-lg-4" key={report.id}>
              <Link to={`/my-reports/${report.id}`} className="report-card card h-100 text-decoration-none">
                <img src={assetUrl(report.imageUrl)} alt="Reported damage" className="report-card__image" />
                <div className="p-3">
                  <div className="d-flex justify-content-between align-items-start mb-2 gap-2">
                    <SeverityPill severity={report.severity} />
                    <StatusPill status={report.status} />
                  </div>
                  <div className="fw-semibold text-truncate">{report.addressText || 'Location not specified'}</div>
                  <div className="small text-muted-app">{formatDate(report.createdAt)}</div>
                </div>
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
