import { useQuery } from '@tanstack/react-query'
import { fetchIncidentById } from '../api/incidents.api'

export function useIncident(id: number) {
  return useQuery({
    queryKey: ['incidents', 'detail', id],
    queryFn: () => fetchIncidentById(id),
  })
}
