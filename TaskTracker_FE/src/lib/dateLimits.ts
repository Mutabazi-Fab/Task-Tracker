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

/** Today's date as YYYY-MM-DD — used as a `min` on any date field that must be today or
 *  later (e.g. an incident's Target Closure Date). Backend-enforced too. */
export function todayDate(): string {
  return new Date().toISOString().slice(0, 10)
}
