import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import EmptyState from '../../components/EmptyState'
import ErrorState from '../../components/ErrorState'
import LoadingState from '../../components/LoadingState'
import SeverityPill from '../../components/SeverityPill'
import StatusPill from '../../components/StatusPill'
import { assetUrl } from '../../api/assetUrl'
import { fetchMyReports } from '../../api/reportApi'

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function MyReportsPage() {
  const [state, setState] = useState({ loading: true, error: '', reports: [] })

  const load = useCallback(() => {
    setState((prev) => ({ ...prev, loading: true, error: '' }))
    fetchMyReports(0, 50)
      .then((data) => setState({ loading: false, error: '', reports: data.content }))
      .catch((err) =>
        setState({ loading: false, error: err.friendlyMessage || 'Failed to load your reports.', reports: [] })
      )
  }, [])

  useEffect(() => {
    load()
  }, [load])

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

      {state.loading && <LoadingState label="Loading your reports…" />}

      {!state.loading && state.error && <ErrorState message={state.error} onRetry={load} />}

      {!state.loading && !state.error && state.reports.length === 0 && (
        <div className="card p-4">
          <EmptyState
            title="No reports yet"
            description="You haven't reported any road damage. Submit your first report to get started."
            action={
              <Link to="/report-damage" className="btn btn-app-primary">
                Report damage
              </Link>
            }
          />
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
