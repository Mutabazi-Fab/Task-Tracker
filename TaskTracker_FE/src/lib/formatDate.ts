/**
 * The backend sends two different shapes of date string:
 *  - date-only ("2026-08-26", e.g. dateAssigned, dashboard trend points) —
 *    JS parses these as UTC midnight, so they must be read back with the
 *    UTC getters or the displayed day can shift near a timezone boundary.
 *  - LocalDateTime ("2026-08-26T09:00:00", e.g. comment/reassignment
 *    timestamps) — no offset, so JS parses it as local time already.
 */
function hasTimeComponent(isoString: string): boolean {
  return isoString.includes('T')
}

/** "2026-08-26" -> "26 AUG 2026". Also accepts a full datetime string. */
export function formatDate(isoString: string): string {
  const date = new Date(isoString)
  if (Number.isNaN(date.getTime())) return isoString

  const dateOnly = !hasTimeComponent(isoString)
  const day = (dateOnly ? date.getUTCDate() : date.getDate()).toString().padStart(2, '0')
  const month = date
    .toLocaleString('en-US', { month: 'short', timeZone: dateOnly ? 'UTC' : undefined })
    .toUpperCase()
  const year = dateOnly ? date.getUTCFullYear() : date.getFullYear()
  return `${day} ${month} ${year}`
}

/** "2026-08-26T09:00:00" -> "26 AUG 2026 09:00". */
export function formatDateTime(isoString: string): string {
  const date = new Date(isoString)
  if (Number.isNaN(date.getTime())) return isoString

  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${formatDate(isoString)} ${hours}:${minutes}`
}
