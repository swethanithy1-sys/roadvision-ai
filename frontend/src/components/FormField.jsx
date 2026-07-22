export default function FormField({ label, error, children, htmlFor }) {
  return (
    <div className="mb-3">
      <label htmlFor={htmlFor} className="form-label fw-semibold">
        {label}
      </label>
      {children}
      {error && <div className="text-danger small mt-1">{error}</div>}
    </div>
  )
}
