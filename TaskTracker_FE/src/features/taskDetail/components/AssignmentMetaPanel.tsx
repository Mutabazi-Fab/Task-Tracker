import { Card } from '../../../components/ui/Card'
import { StatusChip } from '../../../components/ui/StatusChip'
import { formatDate } from '../../../lib/formatDate'
import type { TaskDetail } from '../../../types/task.types'
import styles from './AssignmentMetaPanel.module.css'

/** Assigned to / by / date / status / reassign count — the at-a-glance ownership facts. */
export function AssignmentMetaPanel({ task }: { task: TaskDetail }) {
  return (
    <Card>
      <div className={styles.grid}>
        <div className={styles.item}>
          <span className={styles.label}>Assigned to</span>
          <span className={styles.value}>
            {task.assigneeName} <span className={styles.type}>({task.assigneeType})</span>
          </span>
        </div>
        <div className={styles.item}>
          <span className={styles.label}>Assigned by</span>
          <span className={styles.value}>{task.assignedByName}</span>
        </div>
        <div className={styles.item}>
          <span className={styles.label}>Date assigned</span>
          <span className={styles.value}>{formatDate(task.dateAssigned)}</span>
        </div>
        <div className={styles.item}>
          <span className={styles.label}>Status</span>
          <StatusChip status={task.status} />
        </div>
        <div className={styles.item}>
          <span className={styles.label}>Reassignments</span>
          <span className={styles.value}>{task.reassignments.length}</span>
        </div>
      </div>
    </Card>
  )
}
