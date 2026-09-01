import { useQuery } from '@tanstack/react-query'
import { fetchRoleChangeActivity } from '../api/people.api'

export function useRoleChangeActivity(requesterId: number) {
  return useQuery({
    queryKey: ['people', 'role-changes', requesterId],
    queryFn: () => fetchRoleChangeActivity(requesterId),
    enabled: Number.isFinite(requesterId),
  })
}
