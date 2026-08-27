import { useQuery } from '@tanstack/react-query'
import { fetchTeamMembers } from '../api/teams.api'

export function useTeamMembers(teamId: number) {
  return useQuery({
    queryKey: ['people', 'by-team', teamId],
    queryFn: () => fetchTeamMembers(teamId),
    enabled: Number.isFinite(teamId),
  })
}
