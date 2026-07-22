import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ErrorState from '../../components/ErrorState'
import LoadingState from '../../components/LoadingState'
import SeverityPill from '../../components/SeverityPill'
import StatusPill from '../../components/StatusPill'
import Toast from '../../components/Toast'
import { assetUrl } from '../../api/assetUrl'
import { fetchAllReports, updateReportPriority, updateReportStatus } from '../../api/reportApi'

const STATUS_OPTIONS = ['SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED']
const PRIORITY_OPTIONS = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']

const STATUS_LABELS = {
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under Review',
  ASSIGNED: 'Assigned',
  IN_PROGRESS: 'In Progress',
  RESOLVED: 'Resolved',
  REJECTED: 'Rejected',
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function RepairManagementPage() {
  const [reports, setReports] = useState([])
  const [statusFilter, setStatusFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [savingId, setSavingId] = useState(null)
  const [toast, setToast] = useState('')

  const load = useCallback(() => {
    setLoading(true)
    fetchAllReports(0, 50, statusFilter || null)
      .then((data) => {
        setReports(data.content)
        setError('')
      })
      .catch((err) => setError(err.friendlyMessage || 'Failed to load reports.'))
      .finally(() => setLoading(false))
  }, [statusFilter])

  useEffect(() => {
    load()
  }, [load])

  async function handleStatusChange(report, status) {
    if (status === report.status) return
    setSavingId(report.id)
    try {
      const note = status === 'RESOLVED' ? 'Repair completed and verified' : `Status updated to ${STATUS_LABELS[status]}`
      const updated = await updateReportStatus(report.id, status, note)
      setReports((prev) => prev.map((r) => (r.id === updated.id ? updated : r)))
      setToast(`Status updated to ${STATUS_LABELS[status]}`)
    } catch (err) {
      setError(err.friendlyMessage || 'Failed to update status.')
    } finally {
      setSavingId(null)
    }
  }

  async function handlePriorityChange(report, priority) {
    if (priority === report.repairPriority) return
    setSavingId(report.id)
    try {
      const updated = await updateReportPriority(report.id, priority)
      setReports((prev) => prev.map((r) => (r.id === updated.id ? updated : r)))
      setToast(`Priority updated to ${priority}`)
    } catch (err) {
      setError(err.friendlyMessage || 'Failed to update priority.')
    } finally {
      setSavingId(null)
    }
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
          <h1 className="h4 fw-bold mb-1">Repair Management</h1>
          <p className="text-muted-app mb-0">Review reports, assign priority, and update repair status.</p>
        </div>
        <select
          className="form-select form-select-sm"
          style={{ width: 'auto' }}
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="">All statuses</option>
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {STATUS_LABELS[s]}
            </option>
          ))}
        </select>
      </div>

      {error && <ErrorState message={error} onRetry={load} />}

      {loading && <LoadingState label="Loading reports…" />}

      {!loading && reports.length === 0 && (
        <div className="card p-4">
          <div className="empty-state">No reports match this filter.</div>
        </div>
      )}

      {!loading && reports.length > 0 && (
        <div className="card p-0 repair-table-card">
          <div className="table-responsive">
            <table className="table repair-table align-middle mb-0">
              <thead>
                <tr>
                  <th>Report</th>
                  <th>Severity</th>
                  <th>Priority</th>
                  <th>Status</th>
                  <th>Est. Cost</th>
                  <th>Work Order</th>
                </tr>
              </thead>
              <tbody>
                {reports.map((report) => (
                  <tr key={report.id} className={savingId === report.id ? 'is-saving' : ''}>
                    <td>
                      <div className="d-flex align-items-center gap-2">
                        <img src={assetUrl(report.imageUrl)} alt="" className="repair-table__thumb" />
                        <div className="min-w-0">
                          <Link to={`/my-reports/${report.id}`} className="fw-semibold text-truncate d-block">
                            {report.addressText || 'Location not specified'}
                          </Link>
                          <div className="small text-muted-app">
                            {report.reporterName} · {formatDate(report.createdAt)}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td>
                      <SeverityPill severity={report.severity} />
                    </td>
                    <td>
                      <select
                        className="form-select form-select-sm"
                        value={report.repairPriority}
                        disabled={savingId === report.id}
                        onChange={(e) => handlePriorityChange(report, e.target.value)}
                      >
                        {PRIORITY_OPTIONS.map((p) => (
                          <option key={p} value={p}>
                            {p}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <select
                        className="form-select form-select-sm"
                        value={report.status}
                        disabled={savingId === report.id}
                        onChange={(e) => handleStatusChange(report, e.target.value)}
                      >
                        {STATUS_OPTIONS.map((s) => (
                          <option key={s} value={s}>
                            {STATUS_LABELS[s]}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td className="text-nowrap">₹{report.estimatedCost}</td>
                    <td>
                      <Link to={`/admin/reports/${report.id}/work-order`} className="btn btn-sm btn-outline-secondary">
                        Work order
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Toast message={toast} onDismiss={() => setToast('')} />
    </div>
  )
}
