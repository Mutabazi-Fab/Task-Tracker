import { useQuery } from '@tanstack/react-query'
import { fetchProgressOverTime } from '../api/dashboard.api'

export interface DateRange {
  from: string
  to: string
}

function toISODate(date: Date): string {
  return date.toISOString().slice(0, 10)
}

/** Defaults to the trailing 30 days ending today. */
function defaultRange(): DateRange {
  const to = new Date()
  const from = new Date(to)
  from.setDate(from.getDate() - 29)
  return { from: toISODate(from), to: toISODate(to) }
}

export function useProgressOverTime(range?: DateRange) {
  const { from, to } = range ?? defaultRange()
  return useQuery({
    queryKey: ['dashboard', 'progress-over-time', from, to],
    queryFn: () => fetchProgressOverTime(from, to),
  })
}
