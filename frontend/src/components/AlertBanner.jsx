export default function AlertBanner({ variant = 'danger', children }) {
  if (!children) return null
  return (
    <div className={`alert alert-${variant} py-2 small mb-3`} role="alert">
      {children}
    </div>
  )
}
