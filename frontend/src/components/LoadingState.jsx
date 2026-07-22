export default function LoadingState({ label = 'Loading…', className = '' }) {
  return (
    <div className={`card p-4 text-center text-muted-app ${className}`}>
      <div
        className="spinner-border spinner-border-sm mx-auto mb-2"
        style={{ color: 'var(--color-primary)' }}
        role="status"
      >
        <span className="visually-hidden">Loading</span>
      </div>
      <div className="small">{label}</div>
    </div>
  )
}
