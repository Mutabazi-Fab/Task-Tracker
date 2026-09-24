import type { TaskSeverity } from '../../types/task.types'
import styles from './SeverityBadge.module.css'

const LABEL: Record<TaskSeverity, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  CRITICAL: 'Critical',
}

const STYLE: Record<TaskSeverity, string> = {
  LOW: styles.low,
  MEDIUM: styles.medium,
  HIGH: styles.high,
  CRITICAL: styles.critical,
}

/** Executive-set-only classification — see Task.severity. */
export function SeverityBadge({ severity }: { severity: TaskSeverity }) {
  return <span className={`${styles.badge} ${STYLE[severity]}`}>{LABEL[severity]}</span>
}
