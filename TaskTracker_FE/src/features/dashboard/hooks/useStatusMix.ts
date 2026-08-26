import { useQuery } from '@tanstack/react-query'
import { fetchStatusMix } from '../api/dashboard.api'

export function useStatusMix() {
  return useQuery({
    queryKey: ['dashboard', 'status-mix'],
    queryFn: fetchStatusMix,
  })
}
