import { useQuery } from '@tanstack/react-query'
import { fetchDirectorTasks } from '../api/dashboard.api'

export function useDirectorTasks(directorId: number, page: number, size: number) {
  return useQuery({
    queryKey: ['dashboard', 'director-tasks', directorId, page, size],
    queryFn: () => fetchDirectorTasks(directorId, page, size),
    enabled: Number.isFinite(directorId),
  })
}
