import { useQuery } from '@tanstack/react-query'
import { fetchDirectorTasks } from '../api/dashboard.api'

export function useDirectorTasks(directorId: number) {
  return useQuery({
    queryKey: ['dashboard', 'director-tasks', directorId],
    queryFn: () => fetchDirectorTasks(directorId),
    enabled: Number.isFinite(directorId),
  })
}
