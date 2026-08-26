import { useQuery } from '@tanstack/react-query'
import { fetchPersonStatistics } from '../api/people.api'

export function usePersonStatistics(personId: number) {
  return useQuery({
    queryKey: ['people', 'statistics', personId],
    queryFn: () => fetchPersonStatistics(personId),
    enabled: Number.isFinite(personId),
  })
}
