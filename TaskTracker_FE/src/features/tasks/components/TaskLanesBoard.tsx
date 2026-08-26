import type { TaskListItem, TaskStatus } from '../../../types/task.types'
import { TaskLaneColumn } from './TaskLaneColumn'
import styles from './TaskLanesBoard.module.css'

const LANES: TaskStatus[] = ['PENDING', 'ONGOING', 'COMPLETED']

/** Three status columns, grouped client-side from one unfiltered task list. */
export function TaskLanesBoard({ tasks }: { tasks: TaskListItem[] }) {
  return (
    <div className={styles.board}>
      {LANES.map((status) => (
        <TaskLaneColumn key={status} status={status} tasks={tasks.filter((task) => task.status === status)} />
      ))}
    </div>
  )
}
