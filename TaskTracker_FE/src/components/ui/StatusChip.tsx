import type { TaskStatus } from '../../types/task.types'
import { statusColorKey } from '../../lib/statusColor'
import styles from './StatusChip.module.css'

const CHIP_CLASS: Record<ReturnType<typeof statusColorKey>, string> = {
  completed: styles.completed,
  ongoing: styles.ongoing,
  pending: styles.pending,
}

/**
 * PENDING / ONGOING / COMPLETED badge. Status is derived from percentage —
 * a task at 0% reads PENDING in the pending (red) colour on purpose: it
 * means stalled and needs a reason on record.
 */
export function StatusChip({ status }: { status: TaskStatus }) {
  return <span className={[styles.chip, CHIP_CLASS[statusColorKey(status)]].join(' ')}>{status}</span>
}
