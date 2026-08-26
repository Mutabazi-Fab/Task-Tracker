import { useQuery } from '@tanstack/react-query'
import { fetchTeams } from '../api/teams.api'

export function useTeams() {
  return useQuery({
    queryKey: ['teams', 'list'],
    queryFn: fetchTeams,
  })
}
