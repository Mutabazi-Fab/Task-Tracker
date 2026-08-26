import { StatusChip } from '../../../components/ui/StatusChip'
import { EmptyState } from '../../../components/ui/EmptyState'
import type { TaskListItem, TaskStatus } from '../../../types/task.types'
import { TaskCard } from './TaskCard'
import styles from './TaskLaneColumn.module.css'

interface TaskLaneColumnProps {
  status: TaskStatus
  tasks: TaskListItem[]
}

/** One column with a header (status + count) and its cards. */
export function TaskLaneColumn({ status, tasks }: TaskLaneColumnProps) {
  return (
    <div className={styles.column}>
      <div className={styles.header}>
        <StatusChip status={status} />
        <span className={styles.count}>{tasks.length}</span>
      </div>
      <div className={styles.cards}>
        {tasks.length === 0 ? <EmptyState title="Nothing here" /> : tasks.map((task) => <TaskCard key={task.id} task={task} />)}
      </div>
    </div>
  )
}
