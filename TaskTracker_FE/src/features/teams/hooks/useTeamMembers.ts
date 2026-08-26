import { useQuery } from '@tanstack/react-query'
import { fetchTeamMembers } from '../api/teams.api'

/**
 * Not in the original hook list — added because TeamMemberChip needs a
 * member id to link to a profile, and neither TeamResponse nor
 * TeamStatisticsResponse carries one (see fetchTeamMembers).
 */
export function useTeamMembers(teamId: number) {
  return useQuery({
    queryKey: ['people', 'by-team', teamId],
    queryFn: () => fetchTeamMembers(teamId),
    enabled: Number.isFinite(teamId),
  })
}
