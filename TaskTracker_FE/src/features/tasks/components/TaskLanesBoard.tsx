import type { TaskSortValue, TaskStatus } from '../../../types/task.types'
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
  sort: TaskSortValue
}

/** Status columns, each fetching and paginating its own tasks independently — see
 *  TaskLaneColumn. Replaced the old "fetch up to 200 tasks once, filter client-side"
 *  approach, which both silently dropped anything past the 200th task and had no way to
 *  page through a lane with more items than fit on screen. */
export function TaskLanesBoard({ assignedPersonId, status, sort }: TaskLanesBoardProps) {
  const lanes = status === 'ALL' ? ALL_LANES : [status]
  const isSingleLane = lanes.length === 1

  return (
    <div className={isSingleLane ? styles.boardSingle : styles.board}>
      {lanes.map((lane) => (
        <TaskLaneColumn
          // sort is folded into the key so switching it remounts every lane fresh — each
          // lane owns its own `page` state, which a prop change alone wouldn't reset.
          key={`${lane}-${sort}`}
          status={lane}
          assignedPersonId={assignedPersonId}
          sort={sort}
          layout={isSingleLane ? 'grid' : 'column'}
        />
      ))}
    </div>
  )
}
