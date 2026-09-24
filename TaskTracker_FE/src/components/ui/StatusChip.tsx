import type { TaskStatus } from '../../types/task.types'
import { statusColorKey } from '../../lib/statusColor'
import styles from './StatusChip.module.css'

const CHIP_CLASS: Record<ReturnType<typeof statusColorKey>, string> = {
  completed: styles.completed,
  ongoing: styles.ongoing,
  pending: styles.pending,
}

/** PENDING / ONGOING / COMPLETED badge. */
export function StatusChip({ status }: { status: TaskStatus }) {
  return <span className={[styles.chip, CHIP_CLASS[statusColorKey(status)]].join(' ')}>{status}</span>
}
