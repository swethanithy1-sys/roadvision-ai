import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import BoundingBoxOverlay from '../../components/BoundingBoxOverlay'
import EmptyState from '../../components/EmptyState'
import LoadingState from '../../components/LoadingState'
import SeverityPill from '../../components/SeverityPill'
import StatusPill from '../../components/StatusPill'
import { assetUrl } from '../../api/assetUrl'
import { fetchReportById, fetchReportTimeline } from '../../api/reportApi'

function formatDateTime(iso) {
  return new Date(iso).toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const STATUS_LABELS = {
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under Review',
  ASSIGNED: 'Assigned',
  IN_PROGRESS: 'In Progress',
  RESOLVED: 'Resolved',
  REJECTED: 'Rejected',
}

export default function ReportDetailsPage() {
  const { id } = useParams()
  const [report, setReport] = useState(null)
  const [timeline, setTimeline] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    setLoading(true)
    setError('')

    Promise.all([fetchReportById(id), fetchReportTimeline(id)])
      .then(([reportData, timelineData]) => {
        setReport(reportData)
        setTimeline(timelineData)
      })
      .catch((err) => setError(err.friendlyMessage || 'Failed to load this report.'))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  if (loading) {
    return <LoadingState label="Loading report…" />
  }

  if (error || !report) {
    return (
      <div className="card p-4">
        <EmptyState
          title="Report not found"
          description={error || 'This report does not exist or you do not have access to it.'}
          action={
            <Link to="/my-reports" className="btn btn-app-primary">
              Back to My Reports
            </Link>
          }
        />
      </div>
    )
  }

  return (
    <div className="row g-4">
      <div className="col-lg-7">
        <div className="card p-4">
          <BoundingBoxOverlay imageUrl={assetUrl(report.imageUrl)} boundingBoxes={report.boundingBoxes} />

          <div className="row g-3 mt-1">
            <div className="col-6 col-md-3">
              <div className="detail-tile">
                <div className="detail-tile__label">Severity</div>
                <SeverityPill severity={report.severity} />
              </div>
            </div>
            <div className="col-6 col-md-3">
              <div className="detail-tile">
                <div className="detail-tile__label">Confidence</div>
                <div className="detail-tile__value">{report.confidenceScore}%</div>
              </div>
            </div>
            <div className="col-6 col-md-3">
              <div className="detail-tile">
                <div className="detail-tile__label">Priority</div>
                <div className="detail-tile__value">{report.repairPriority}</div>
              </div>
            </div>
            <div className="col-6 col-md-3">
              <div className="detail-tile">
                <div className="detail-tile__label">Est. Cost</div>
                <div className="detail-tile__value">₹{report.estimatedCost}</div>
              </div>
            </div>
          </div>
        </div>

        <div className="card p-4 mt-3">
          <h2 className="h6 fw-semibold mb-3">Details</h2>
          <dl className="detail-list mb-0">
            <dt>Damage type</dt>
            <dd>{report.damageType.replace('_', ' ')}</dd>
            <dt>Location</dt>
            <dd>
              {report.addressText || 'Not specified'}
              {report.latitude && report.longitude ? ` (${report.latitude}, ${report.longitude})` : ''}
            </dd>
            <dt>Description</dt>
            <dd>{report.description || '—'}</dd>
            <dt>Reported by</dt>
            <dd>{report.reporterName}</dd>
            <dt>Submitted</dt>
            <dd>{formatDateTime(report.createdAt)}</dd>
          </dl>
        </div>
      </div>

      <div className="col-lg-5">
        <div className="card p-4 mb-3">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h2 className="h6 fw-semibold mb-0">Current Status</h2>
            <StatusPill status={report.status} />
          </div>
        </div>

        <div className="card p-4">
          <h2 className="h6 fw-semibold mb-3">Timeline</h2>
          <ul className="timeline mb-0">
            {timeline.map((entry, index) => (
              <li key={index} className="timeline__item">
                <div className="timeline__dot" />
                <div>
                  <div className="fw-semibold small">{STATUS_LABELS[entry.status] || entry.status}</div>
                  {entry.note && <div className="small text-muted-app">{entry.note}</div>}
                  <div className="small text-muted-app">
                    {entry.changedByName} · {formatDateTime(entry.changedAt)}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}
