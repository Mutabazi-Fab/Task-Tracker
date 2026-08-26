import { useQuery } from '@tanstack/react-query'
import { fetchTeamTasks } from '../api/teams.api'

export function useTeamTasks(teamId: number) {
  return useQuery({
    queryKey: ['teams', 'tasks', teamId],
    queryFn: () => fetchTeamTasks(teamId),
    enabled: Number.isFinite(teamId),
  })
}
