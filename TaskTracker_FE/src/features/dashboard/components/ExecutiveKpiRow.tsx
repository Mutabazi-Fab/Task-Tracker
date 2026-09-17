import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { StatCard } from '../../../components/ui/StatCard'
import { formatPercentage } from '../../../lib/formatPercentage'
import { useExecutiveKpis } from '../hooks/useExecutiveKpis'
import styles from './KpiRow.module.css'

/** The four org-health tiles atop the Executive Dashboard — its own component/data source,
 *  not KpiRow/useDashboardOverview, since a Director's dashboard still needs the old ones
 *  unchanged and these four numbers don't exist in DashboardOverview. Reuses KpiRow's own
 *  grid layout (same 4-tile shape). */
export function ExecutiveKpiRow() {
  const query = useExecutiveKpis()

  return (
    <QueryBoundary query={query}>
      {(kpis) => (
        <div className={styles.row}>
          <StatCard compact label="Departments on track" value={formatPercentage(kpis.orgOnTrackPercentage)} />
          <StatCard compact label="Overdue" value={String(kpis.overdueCount)} note="top-level initiatives" />
          <StatCard compact label="Critical, not complete" value={String(kpis.criticalPendingCount)} />
          <StatCard compact label="Awaiting your decision" value={String(kpis.pendingDecisionsCount)} note="deadline extensions" />
        </div>
      )}
    </QueryBoundary>
  )
}
