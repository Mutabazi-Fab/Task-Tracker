import type { TaskStatus } from '../../../types/task.types'
import type { TaskStatusFilterValue } from './TaskStatusFilter'
import { TaskLaneColumn } from './TaskLaneColumn'
import styles from './TaskLanesBoard.module.css'

const ALL_LANES: TaskStatus[] = ['PENDING', 'ONGOING', 'COMPLETED']

interface TaskLanesBoardProps {
  assignedPersonId?: number
  /** Same status filter the Table view already respects — 'ALL' shows all three columns,
   *  anything else narrows the board down to just that one column, matching what picking
   *  that tab already does on the Table side. */
  status: TaskStatusFilterValue
}

/** Status columns, each fetching and paginating its own tasks independently — see
 *  TaskLaneColumn. Replaced the old "fetch up to 200 tasks once, filter client-side"
 *  approach, which both silently dropped anything past the 200th task and had no way to
 *  page through a lane with more items than fit on screen. */
export function TaskLanesBoard({ assignedPersonId, status }: TaskLanesBoardProps) {
  const lanes = status === 'ALL' ? ALL_LANES : [status]
  const isSingleLane = lanes.length === 1

  return (
    <div className={isSingleLane ? styles.boardSingle : styles.board}>
      {lanes.map((lane) => (
        <TaskLaneColumn
          key={lane}
          status={lane}
          assignedPersonId={assignedPersonId}
          layout={isSingleLane ? 'grid' : 'column'}
        />
      ))}
    </div>
  )
}
