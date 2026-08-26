import { useQuery } from '@tanstack/react-query'
import { fetchPeople } from '../api/people.api'

export function usePeople() {
  return useQuery({
    queryKey: ['people', 'list'],
    queryFn: fetchPeople,
  })
}
