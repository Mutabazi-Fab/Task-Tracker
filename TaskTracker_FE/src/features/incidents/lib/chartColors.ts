/** A rotating palette built entirely from this app's own design tokens — greens plus the
 *  handful of accent hues already used elsewhere (khaki, signal-info, signal-exec, status
 *  tones) — so a multi-bar chart reads as distinct series without introducing any colour
 *  the rest of the UI doesn't already use. */
const PALETTE = [
  'var(--accent)',
  'var(--signal-info)',
  'var(--khaki)',
  'var(--signal-exec)',
  'var(--accent-strong)',
  'var(--status-ongoing)',
  'var(--status-pending)',
  'var(--signal-admin)',
  'var(--status-completed)',
  'var(--muted)',
  'var(--accent-soft)',
]

export function paletteColor(index: number): string {
  return PALETTE[index % PALETTE.length]
}

/** Reuses the exact hues IncidentStatusBadge already uses for each status, so the bar chart
 *  and the badges never disagree about what colour "Monitoring" or "Closed" is. */
const STATUS_COLORS: Record<string, string> = {
  OPEN: 'var(--signal-info)',
  UNDER_INVESTIGATION: 'var(--signal-exec)',
  MONITORING: 'var(--status-ongoing)',
  CLOSED: 'var(--status-completed)',
  REJECTED_NOT_AN_INCIDENT: 'var(--muted)',
}

export function statusChartColor(label: string): string {
  return STATUS_COLORS[label] ?? 'var(--accent)'
}

/** Reuses the exact hues IncidentSeverityBadge already uses for each severity. */
const SEVERITY_COLORS: Record<string, string> = {
  LOW: 'var(--muted)',
  MODERATE: 'var(--signal-info)',
  HIGH: 'var(--signal-exec)',
  CRITICAL: 'var(--status-pending)',
}

export function severityChartColor(label: string): string {
  return SEVERITY_COLORS[label] ?? 'var(--accent)'
}
