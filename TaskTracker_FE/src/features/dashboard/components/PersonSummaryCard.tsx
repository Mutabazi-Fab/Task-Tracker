import { Card } from '../../../components/ui/Card'
import { Avatar } from '../../../components/ui/Avatar'
import { formatPercentage } from '../../../lib/formatPercentage'
import type { PersonSummary } from '../../../types/dashboard.types'
import styles from './PersonSummaryCard.module.css'

export function PersonSummaryCard({ person }: { person: PersonSummary }) {
  return (
    <Card padding="sm">
      <div className={styles.top}>
        <Avatar name={person.name} />
        <div className={styles.identity}>
          <span className={styles.name}>{person.name}</span>
          <span className={styles.role}>{person.role}</span>
        </div>
      </div>
      <div className={styles.stats}>
        <div className={styles.stat}>
          <span className={styles.statValue}>{formatPercentage(person.averageProgress)}</span>
          <span className={styles.statLabel}>Avg</span>
        </div>
        <div className={styles.stat}>
          <span className={styles.statValue}>{person.assignedCount}</span>
          <span className={styles.statLabel}>Assigned</span>
        </div>
        <div className={styles.stat}>
          <span className={styles.statValue}>{person.completedCount}</span>
          <span className={styles.statLabel}>Done</span>
        </div>
      </div>
    </Card>
  )
}
