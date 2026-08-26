import { useQuery } from '@tanstack/react-query'
import { fetchPeopleSummary } from '../api/dashboard.api'

export function usePeopleSummary() {
  return useQuery({
    queryKey: ['dashboard', 'people-summary'],
    queryFn: fetchPeopleSummary,
  })
}
