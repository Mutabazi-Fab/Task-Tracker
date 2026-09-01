import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { fetchTasks } from '../api/tasks.api'
import type { TaskStatus } from '../../../types/task.types'

interface UseTasksParams {
  status?: TaskStatus
  assignedPersonId?: number
  page: number
  size: number
}

/** The paginated task list, with an optional status filter and an optional
 *  assignedPersonId scope (see FetchTasksParams). */
export function useTasks({ status, assignedPersonId, page, size }: UseTasksParams) {
  return useQuery({
    queryKey: ['tasks', 'list', status ?? 'ALL', assignedPersonId ?? 'ALL', page, size],
    queryFn: () => fetchTasks({ status, assignedPersonId, page, size }),
    placeholderData: keepPreviousData,
  })
}
