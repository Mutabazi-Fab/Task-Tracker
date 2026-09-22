/** Caps how far into the future "date assigned" can be set — a typo guard only (e.g.
 *  2036 instead of 2026). Backend-enforced independently too (requireReasonableDate). */
export function maxAssignableDate(): string {
  const d = new Date()
  d.setFullYear(d.getFullYear() + 1)
  return d.toISOString().slice(0, 10)
}

/** Caps how far into the past "date assigned" can be set — genuine backfilling fits
 *  inside 3 months; anything older skews "how old is this task" reporting. Backend-enforced too. */
export function minAssignableDate(): string {
  const d = new Date()
  d.setMonth(d.getMonth() - 3)
  return d.toISOString().slice(0, 10)
}
