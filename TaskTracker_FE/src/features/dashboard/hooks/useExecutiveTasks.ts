import { useQuery } from '@tanstack/react-query'
import { fetchExecutiveTasks } from '../api/dashboard.api'
import type { TaskSortValue } from '../../../types/task.types'

export function useExecutiveTasks(page: number, size: number, sort: TaskSortValue) {
  return useQuery({
    queryKey: ['dashboard', 'executive-tasks', page, size, sort],
    queryFn: () => fetchExecutiveTasks(page, size, sort),
  })
}
