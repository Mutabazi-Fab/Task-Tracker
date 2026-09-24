import { StatusChip } from '../../../components/ui/StatusChip'
import type { TaskStatus } from '../../../types/task.types'
import { useTasks } from '../hooks/useTasks'
import styles from './TaskStatusSummary.module.css'

const STATUSES: TaskStatus[] = ['PENDING', 'ONGOING', 'COMPLETED']

interface TaskStatusSummaryProps {
  assignedPersonId?: number
}

/** The same "status + total count" the Lanes view already shows per column header, shown as one row
 *  above the Table view instead — Table only ever showed the current page's rows, with no sense of how
 *  many of each status exist overall. */
export function TaskStatusSummary({ assignedPersonId }: TaskStatusSummaryProps) {
  const pending = useTasks({ status: 'PENDING', assignedPersonId, page: 0, size: 1 })
  const ongoing = useTasks({ status: 'ONGOING', assignedPersonId, page: 0, size: 1 })
  const completed = useTasks({ status: 'COMPLETED', assignedPersonId, page: 0, size: 1 })
  const queries = { PENDING: pending, ONGOING: ongoing, COMPLETED: completed }

  return (
    <div className={styles.wrap}>
      {STATUSES.map((status) => (
        <div key={status} className={styles.item}>
          <StatusChip status={status} />
          <span className={styles.count}>{queries[status].data?.totalElements ?? '…'}</span>
        </div>
      ))}
    </div>
  )
}
