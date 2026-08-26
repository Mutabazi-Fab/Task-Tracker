import { useQuery } from '@tanstack/react-query'
import { fetchPersonTaskHistory } from '../api/people.api'

export function usePersonTaskHistory(personId: number) {
  return useQuery({
    queryKey: ['people', 'task-history', personId],
    queryFn: () => fetchPersonTaskHistory(personId),
    enabled: Number.isFinite(personId),
  })
}
