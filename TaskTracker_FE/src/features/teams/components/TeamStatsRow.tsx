import { StatCard } from '../../../components/ui/StatCard'
import { formatPercentage } from '../../../lib/formatPercentage'
import type { TeamStatistics } from '../../../types/team.types'
import styles from './TeamStatsRow.module.css'

export function TeamStatsRow({ stats }: { stats: TeamStatistics }) {
  return (
    <div className={styles.row}>
      <StatCard label="Average progress" value={formatPercentage(stats.averageProgress)} />
      <StatCard label="Tasks" value={String(stats.taskCount)} />
      <StatCard label="Members" value={String(stats.memberCount)} />
      <StatCard label="Completed" value={String(stats.completedCount)} />
    </div>
  )
}
