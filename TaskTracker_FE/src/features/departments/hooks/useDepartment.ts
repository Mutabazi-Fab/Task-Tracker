import { useQuery } from '@tanstack/react-query'
import { fetchDepartment } from '../api/departments.api'

export function useDepartment(id: number) {
  return useQuery({
    queryKey: ['departments', 'detail', id],
    queryFn: () => fetchDepartment(id),
    enabled: Number.isFinite(id),
  })
}
