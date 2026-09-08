/** Caps how far into the future a "date assigned" field can be set — a typo guard only
 *  (e.g. picking 2036 instead of 2026), not a real business rule: past dates are always
 *  fine (backfilling real assignments is normal), and there's no due-date/deadline concept
 *  in this app for a future date to violate. The backend enforces the same one-year bound
 *  independently (TaskServiceImpl) — this is just what keeps the date picker itself from
 *  offering the mistake in the first place. */
export function maxAssignableDate(): string {
  const d = new Date()
  d.setFullYear(d.getFullYear() + 1)
  return d.toISOString().slice(0, 10)
}
