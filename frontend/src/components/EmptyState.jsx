export default function EmptyState({ title, description, action }) {
  return (
    <div className="empty-state">
      {title && <h2 className="h6 fw-semibold mb-2">{title}</h2>}
      {description && <p className="mb-3 small">{description}</p>}
      {action}
    </div>
  )
}
