import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { fetchDepartmentActivity } from '../api/departments.api'

export function useDepartmentActivity(page: number, size: number) {
  return useQuery({
    queryKey: ['departments', 'activity', page, size],
    queryFn: () => fetchDepartmentActivity(page, size),
    placeholderData: keepPreviousData,
  })
}
