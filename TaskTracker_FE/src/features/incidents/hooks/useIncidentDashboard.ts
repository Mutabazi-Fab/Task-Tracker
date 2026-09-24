import { useQuery } from '@tanstack/react-query'
import { fetchIncidentDashboard } from '../api/incidents.api'

export function useIncidentDashboard() {
  return useQuery({
    queryKey: ['incidents', 'dashboard'],
    queryFn: fetchIncidentDashboard,
  })
}
