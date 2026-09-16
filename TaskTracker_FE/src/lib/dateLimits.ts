/** Caps how far into the future a "date assigned" field can be set — a typo guard only
 *  (e.g. picking 2036 instead of 2026), not a real business rule: there's no due-date/
 *  deadline concept for a future date to violate on its own (see requireDeadlineNotBefore
 *  Assignment for the actual deadline-vs-assignment check). The backend enforces the same
 *  one-year bound independently (TaskServiceImpl.requireReasonableDate) — this is just what
 *  keeps the date picker itself from offering the mistake in the first place. */
export function maxAssignableDate(): string {
  const d = new Date()
  d.setFullYear(d.getFullYear() + 1)
  return d.toISOString().slice(0, 10)
}

/** Caps how far into the past a "date assigned" field can be set — a real integrity rule,
 *  not just a typo guard: genuine backfilling (recording a task that actually started last
 *  quarter, before anyone got around to entering it) fits comfortably inside 3 months;
 *  anything older reads as a mistaken date and skews "how old is this task" reporting/audit
 *  history. Backend-enforced independently (TaskServiceImpl.requireReasonableDate) — this
 *  is just what keeps the date picker from offering the mistake in the first place. */
export function minAssignableDate(): string {
  const d = new Date()
  d.setMonth(d.getMonth() - 3)
  return d.toISOString().slice(0, 10)
}
