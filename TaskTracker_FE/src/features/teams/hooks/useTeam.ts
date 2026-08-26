import { useQuery } from '@tanstack/react-query'
import { fetchTeam } from '../api/teams.api'

export function useTeam(teamId: number) {
  return useQuery({
    queryKey: ['teams', 'detail', teamId],
    queryFn: () => fetchTeam(teamId),
    enabled: Number.isFinite(teamId),
  })
}
