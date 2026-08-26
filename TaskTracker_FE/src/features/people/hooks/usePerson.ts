import { useQuery } from '@tanstack/react-query'
import { fetchPerson } from '../api/people.api'

export function usePerson(personId: number) {
  return useQuery({
    queryKey: ['people', 'detail', personId],
    queryFn: () => fetchPerson(personId),
    enabled: Number.isFinite(personId),
  })
}
