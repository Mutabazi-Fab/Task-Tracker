import { useQuery } from '@tanstack/react-query'
import { fetchTeamStatistics } from '../api/teams.api'

/** enabled defaults true — TeamPage passes false when the viewer isn't allowed to see this
 *  team's full detail (not a Director/Super Admin, not a member of this team). */
export function useTeamStatistics(teamId: number, enabled: boolean = true) {
  return useQuery({
    queryKey: ['teams', 'statistics', teamId],
    queryFn: () => fetchTeamStatistics(teamId),
    enabled: Number.isFinite(teamId) && enabled,
  })
}
