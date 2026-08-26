import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { TaskTable } from '../../tasks/components/TaskTable'
import { useTeamTasks } from '../hooks/useTeamTasks'

/**
 * Reuses the tasks feature's TaskTable rather than duplicating a second
 * table/mobile-row renderer — it already takes a plain TaskListItem[] and
 * carries no dependency on the tasks feature's own state.
 */
export function TeamTaskList({ teamId }: { teamId: number }) {
  const query = useTeamTasks(teamId)
  return <QueryBoundary query={query}>{(tasks) => <TaskTable tasks={tasks} />}</QueryBoundary>
}
