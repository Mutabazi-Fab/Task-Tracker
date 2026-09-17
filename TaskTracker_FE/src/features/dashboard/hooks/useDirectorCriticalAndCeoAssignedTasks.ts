import { useQuery } from '@tanstack/react-query'
import { fetchDirectorCriticalAndCeoAssignedTasks } from '../api/dashboard.api'
import type { TaskSortValue } from '../../../types/task.types'

export function useDirectorCriticalAndCeoAssignedTasks(page: number, size: number, sort: TaskSortValue, enabled: boolean) {
  return useQuery({
    queryKey: ['dashboard', 'director-critical-and-ceo-assigned-tasks', page, size, sort],
    queryFn: () => fetchDirectorCriticalAndCeoAssignedTasks(page, size, sort),
    enabled,
  })
}
