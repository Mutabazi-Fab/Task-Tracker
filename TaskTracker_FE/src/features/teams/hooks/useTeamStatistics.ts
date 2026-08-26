import { useQuery } from '@tanstack/react-query'
import { fetchTeamStatistics } from '../api/teams.api'

export function useTeamStatistics(teamId: number) {
  return useQuery({
    queryKey: ['teams', 'statistics', teamId],
    queryFn: () => fetchTeamStatistics(teamId),
    enabled: Number.isFinite(teamId),
  })
}
