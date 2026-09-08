import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { fetchTasks } from '../api/tasks.api'
import type { TaskSortValue, TaskStatus } from '../../../types/task.types'

interface UseTasksParams {
  status?: TaskStatus
  assignedPersonId?: number
  page: number
  size: number
  sort?: TaskSortValue
}

/** The paginated task list, with an optional status filter, an optional assignedPersonId
 *  scope, and a sort order (see TaskSortValue) — defaults to most-recently-updated first. */
export function useTasks({ status, assignedPersonId, page, size, sort = 'updatedAt,desc' }: UseTasksParams) {
  return useQuery({
    queryKey: ['tasks', 'list', status ?? 'ALL', assignedPersonId ?? 'ALL', page, size, sort],
    queryFn: () => fetchTasks({ status, assignedPersonId, page, size, sort }),
    placeholderData: keepPreviousData,
  })
}
