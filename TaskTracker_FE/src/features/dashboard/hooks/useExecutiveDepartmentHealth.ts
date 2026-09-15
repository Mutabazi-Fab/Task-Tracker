import { useQuery } from '@tanstack/react-query'
import { fetchExecutiveDepartmentHealth } from '../api/dashboard.api'

export function useExecutiveDepartmentHealth() {
  return useQuery({
    queryKey: ['dashboard', 'executive', 'department-health'],
    queryFn: fetchExecutiveDepartmentHealth,
  })
}
