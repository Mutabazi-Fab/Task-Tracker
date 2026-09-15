import { useQuery } from '@tanstack/react-query'
import { fetchExecutiveKpis } from '../api/dashboard.api'

export function useExecutiveKpis() {
  return useQuery({
    queryKey: ['dashboard', 'executive', 'kpis'],
    queryFn: fetchExecutiveKpis,
  })
}
