const STATUS_META = {
  SUBMITTED: { label: 'Submitted', tone: 'medium' },
  UNDER_REVIEW: { label: 'Under Review', tone: 'medium' },
  ASSIGNED: { label: 'Assigned', tone: 'medium' },
  IN_PROGRESS: { label: 'In Progress', tone: 'medium' },
  RESOLVED: { label: 'Resolved', tone: 'low' },
  REJECTED: { label: 'Rejected', tone: 'high' },
}

export default function StatusPill({ status }) {
  const meta = STATUS_META[status] || { label: status, tone: 'medium' }
  return <span className={`severity-pill ${meta.tone}`}>{meta.label}</span>
}
