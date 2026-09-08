import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { fetchTaskActivity } from '../api/tasks.api'

export function useTaskActivity(page: number, size: number) {
  return useQuery({
    queryKey: ['tasks', 'activity', page, size],
    queryFn: () => fetchTaskActivity(page, size),
    placeholderData: keepPreviousData,
  })
}
