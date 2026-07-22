import { useEffect } from 'react'

export default function Toast({ message, onDismiss, variant = 'success' }) {
  useEffect(() => {
    if (!message) return undefined
    const timer = setTimeout(onDismiss, 3000)
    return () => clearTimeout(timer)
  }, [message, onDismiss])

  if (!message) return null

  return (
    <div className={`app-toast app-toast--${variant}`} role="status">
      {message}
    </div>
  )
}
