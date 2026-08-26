import { TaskTable } from '../../tasks/components/TaskTable'
import type { TaskListItem } from '../../../types/task.types'

/** Reuses TaskTable — same reasoning as TeamTaskList: plain TaskListItem[] in, no shared state. */
export function TaskResultsSection({ tasks }: { tasks: TaskListItem[] }) {
  return <TaskTable tasks={tasks} />
}
