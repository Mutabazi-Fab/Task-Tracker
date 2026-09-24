import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { StatCard } from '../../../components/ui/StatCard'
import { formatCurrency } from '../lib/incidentLabels'
import { useIncidentDashboard } from '../hooks/useIncidentDashboard'
import styles from './IncidentKpiRow.module.css'

/** Reproduces every tile the source Excel's Dashboard sheet computed: total, open/monitoring,
 *  critical/high, overdue-actions, plus the financial row (gross/recovery/net loss,
 *  regulator-notifiable count). */
export function IncidentKpiRow() {
  const query = useIncidentDashboard()

  return (
    <QueryBoundary query={query}>
      {(dashboard) => (
        <>
          <div className={styles.row}>
            <StatCard compact label="Total incidents" value={String(dashboard.totalIncidents)} />
            <StatCard compact label="Open / monitoring" value={String(dashboard.openOrMonitoringCount)} />
            <StatCard compact label="Closed incidents" value={String(dashboard.closedCount)} />
            <StatCard compact label="Critical / high" value={String(dashboard.criticalOrHighCount)} />
            <StatCard compact label="Overdue actions" value={String(dashboard.overdueActionsCount)} />
          </div>
          <div className={styles.row}>
            <StatCard compact label="Gross loss" value={formatCurrency(dashboard.grossLossTotal)} />
            <StatCard compact label="Recoveries" value={formatCurrency(dashboard.recoveryTotal)} />
            <StatCard compact label="Net loss" value={formatCurrency(dashboard.netLossTotal)} />
            <StatCard compact label="Regulator-notifiable" value={String(dashboard.regulatorNotifiableCount)} />
          </div>
        </>
      )}
    </QueryBoundary>
  )
}
