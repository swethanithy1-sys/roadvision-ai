export default function StatCard({ label, value, accent = 'primary', hint }) {
  return (
    <div className="card p-4 stat-card">
      <div className={`stat-card__icon stat-card__icon--${accent}`} aria-hidden="true" />
      <div className="stat-card__value">{value}</div>
      <div className="stat-card__label">{label}</div>
      {hint && <div className="stat-card__hint">{hint}</div>}
    </div>
  )
}
