import type { TeamLeaderboardItem } from '../../../types/dashboard.types'
import { formatPercentage } from '../../../lib/formatPercentage'
import styles from './TeamLeaderboardRow.module.css'

/**
 * A team's average progress is an aggregate across many tasks, not one
 * task's status — so this bar uses --accent, not a --status-* colour.
 */
export function TeamLeaderboardRow({ team }: { team: TeamLeaderboardItem }) {
  const clamped = Math.min(100, Math.max(0, team.averageProgress ?? 0))

  return (
    <div className={styles.row}>
      <div className={styles.name}>
        <span className={styles.teamName}>{team.name}</span>
        <span className={styles.leader}>{team.leaderName ? `Led by ${team.leaderName}` : 'No leader assigned'}</span>
      </div>
      <div className={styles.bar}>
        <div className={styles.track}>
          <div className={styles.fill} style={{ width: `${clamped}%` }} />
        </div>
        <span className={styles.percentage}>{formatPercentage(team.averageProgress)}</span>
      </div>
      <span className={styles.count}>{team.taskCount}</span>
      <span className={styles.count}>{team.completedCount}</span>
    </div>
  )
}
