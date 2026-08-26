import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { fetchTasks } from '../api/tasks.api'
import type { TaskStatus } from '../../../types/task.types'

interface UseTasksParams {
  status?: TaskStatus
  page: number
  size: number
}

/** The paginated task list, with an optional status filter. */
export function useTasks({ status, page, size }: UseTasksParams) {
  return useQuery({
    queryKey: ['tasks', 'list', status ?? 'ALL', page, size],
    queryFn: () => fetchTasks({ status, page, size }),
    placeholderData: keepPreviousData,
  })
}
