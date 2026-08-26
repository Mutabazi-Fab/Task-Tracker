/** 54.333 -> "54%". null/undefined (no data yet) -> "—". */
export function formatPercentage(value: number | null | undefined, decimals = 0): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '—'
  return `${value.toFixed(decimals)}%`
}
