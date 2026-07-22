const LABELS = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High' }

export default function SeverityPill({ severity }) {
  if (!severity) return null
  return <span className={`severity-pill ${severity.toLowerCase()}`}>{LABELS[severity] || severity}</span>
}
