import { useQuery } from '@tanstack/react-query'
import { fetchTeamLeaderboard } from '../api/dashboard.api'

export function useTeamLeaderboard() {
  return useQuery({
    queryKey: ['dashboard', 'team-leaderboard'],
    queryFn: fetchTeamLeaderboard,
  })
}
