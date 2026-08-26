import { Card } from '../../../components/ui/Card'
import { ProgressBar } from '../../../components/ui/ProgressBar'
import { formatPercentage } from '../../../lib/formatPercentage'
import type { TaskStatus } from '../../../types/task.types'
import styles from './TaskProgressPanel.module.css'

interface TaskProgressPanelProps {
  percentage: number
  status: TaskStatus
}

/** Big bar + percentage. Read-only — progress only moves via a comment. */
export function TaskProgressPanel({ percentage, status }: TaskProgressPanelProps) {
  return (
    <Card>
      <div className={styles.top}>
        <span className={styles.label}>Progress</span>
        <span className={styles.value}>{formatPercentage(percentage)}</span>
      </div>
      <ProgressBar percentage={percentage} status={status} />
    </Card>
  )
}
