import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { StatCard } from '../../../components/ui/StatCard'
import { formatPercentage } from '../../../lib/formatPercentage'
import { useDashboardOverview } from '../hooks/useDashboardOverview'
import styles from './KpiRow.module.css'

function shareOfTotal(count: number, total: number): string {
  if (total === 0) return '—'
  return `${formatPercentage((count / total) * 100)} of total`
}

/** The four headline stat cards atop the dashboard. */
export function KpiRow() {
  const query = useDashboardOverview()

  return (
    <QueryBoundary query={query}>
      {(overview) => (
        <div className={styles.row}>
          <StatCard
            label="Org average progress"
            value={formatPercentage(overview.orgAverageProgress)}
            note={`${overview.totalTasks} tasks`}
          />
          <StatCard
            label="Completed"
            value={String(overview.completedCount)}
            note={shareOfTotal(overview.completedCount, overview.totalTasks)}
          />
          <StatCard
            label="Ongoing"
            value={String(overview.ongoingCount)}
            note={shareOfTotal(overview.ongoingCount, overview.totalTasks)}
          />
          <StatCard label="Pending" value={String(overview.pendingCount)} note="stalled at 0%" />
        </div>
      )}
    </QueryBoundary>
  )
}
