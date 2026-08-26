import { StatCard } from '../../../components/ui/StatCard'
import { formatPercentage } from '../../../lib/formatPercentage'
import type { PersonStatistics } from '../../../types/person.types'
import styles from './PersonStatsRow.module.css'

/** Avg progress, assigned, completed, comments logged. */
export function PersonStatsRow({ stats }: { stats: PersonStatistics }) {
  return (
    <div className={styles.row}>
      <StatCard label="Average progress" value={formatPercentage(stats.averageProgress)} />
      <StatCard label="Assigned" value={String(stats.tasksAssigned)} note={`${stats.tasksOngoing} ongoing`} />
      <StatCard label="Completed" value={String(stats.tasksCompleted)} note={`${stats.tasksPending} stalled at 0%`} />
      <StatCard label="Comments logged" value={String(stats.commentsLogged)} note={`${stats.tasksHandedOff} handed off`} />
    </div>
  )
}
