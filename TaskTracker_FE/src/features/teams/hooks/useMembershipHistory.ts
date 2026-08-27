import { useQuery } from '@tanstack/react-query'
import { fetchMembershipHistory } from '../api/teams.api'

export function useMembershipHistory(teamId: number) {
  return useQuery({
    queryKey: ['teams', 'membership-history', teamId],
    queryFn: () => fetchMembershipHistory(teamId),
    enabled: Number.isFinite(teamId),
  })
}
