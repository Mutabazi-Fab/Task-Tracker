import { useQuery } from '@tanstack/react-query'
import { fetchDirectorTasks } from '../api/dashboard.api'
import type { TaskSortValue } from '../../../types/task.types'

export function useDirectorTasks(directorId: number, page: number, size: number, sort: TaskSortValue) {
  return useQuery({
    queryKey: ['dashboard', 'director-tasks', directorId, page, size, sort],
    queryFn: () => fetchDirectorTasks(directorId, page, size, sort),
    enabled: Number.isFinite(directorId),
  })
}
