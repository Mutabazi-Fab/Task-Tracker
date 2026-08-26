import { formatDateTime } from '../../../lib/formatDate'
import type { TaskReassignment } from '../../../types/reassignment.types'
import styles from './ReassignmentHistoryItem.module.css'

/** From -> to, by whom, when, why. Immutable — no edit/delete affordance. */
export function ReassignmentHistoryItem({ reassignment }: { reassignment: TaskReassignment }) {
  return (
    <div className={styles.item}>
      <div className={styles.transfer}>
        <span className={styles.name}>{reassignment.fromName}</span>
        <span className={styles.arrow}>→</span>
        <span className={styles.name}>{reassignment.toName}</span>
      </div>
      <p className={styles.reason}>{reassignment.reason}</p>
      <p className={styles.meta}>
        {reassignment.reassignedByName} · {formatDateTime(reassignment.reassignedAt)}
      </p>
    </div>
  )
}
