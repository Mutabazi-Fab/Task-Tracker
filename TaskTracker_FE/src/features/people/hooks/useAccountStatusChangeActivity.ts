import { useQuery } from '@tanstack/react-query'
import { fetchAccountStatusChangeActivity } from '../api/people.api'

export function useAccountStatusChangeActivity(requesterId: number) {
  return useQuery({
    queryKey: ['people', 'account-status-changes', requesterId],
    queryFn: () => fetchAccountStatusChangeActivity(requesterId),
    enabled: Number.isFinite(requesterId),
  })
}
