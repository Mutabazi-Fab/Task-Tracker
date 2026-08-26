import type { TaskStatus } from '../../types/task.types'
import { statusColorKey } from '../../lib/statusColor'
import styles from './ProgressBar.module.css'

interface ProgressBarProps {
  percentage: number
  status: TaskStatus
}

const FILL_CLASS: Record<ReturnType<typeof statusColorKey>, string> = {
  completed: styles.fillCompleted,
  ongoing: styles.fillOngoing,
  pending: styles.fillPending,
}

/**
 * Track + fill. The fill always uses the status colour of the task it
 * belongs to — one of the three places in the whole UI allowed to.
 */
export function ProgressBar({ percentage, status }: ProgressBarProps) {
  const clamped = Math.min(100, Math.max(0, percentage))
  return (
    <div className={styles.track} role="progressbar" aria-valuenow={clamped} aria-valuemin={0} aria-valuemax={100}>
      <div className={FILL_CLASS[statusColorKey(status)]} style={{ width: `${clamped}%` }} />
    </div>
  )
}
