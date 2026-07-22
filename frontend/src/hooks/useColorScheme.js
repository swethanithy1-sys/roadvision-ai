import { useEffect, useState } from 'react'

function readScheme() {
  const explicit = document.documentElement.getAttribute('data-theme')
  if (explicit === 'dark' || explicit === 'light') return explicit
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/** Tracks the active light/dark scheme, including the user's manual toggle (data-theme attribute). */
export function useColorScheme() {
  const [scheme, setScheme] = useState(readScheme)

  useEffect(() => {
    const update = () => setScheme(readScheme())

    const observer = new MutationObserver(update)
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })

    const media = window.matchMedia('(prefers-color-scheme: dark)')
    media.addEventListener('change', update)

    return () => {
      observer.disconnect()
      media.removeEventListener('change', update)
    }
  }, [])

  return scheme
}
