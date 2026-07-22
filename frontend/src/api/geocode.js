/**
 * Reverse geocoding via OpenStreetMap's free Nominatim API — same free stack as the
 * Leaflet/OSM hazard map, no API key needed. Usage policy caps this at ~1 request/second,
 * which is inherently satisfied here since it only fires on a manual button click.
 */
export async function reverseGeocode(latitude, longitude) {
  const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${latitude}&lon=${longitude}&zoom=18&addressdetails=1`

  const response = await fetch(url, {
    headers: { Accept: 'application/json' },
  })

  if (!response.ok) {
    throw new Error(`Reverse geocoding failed with status ${response.status}`)
  }

  const data = await response.json()
  if (!data || !data.display_name) {
    throw new Error('No address found for this location')
  }

  return data.display_name
}
