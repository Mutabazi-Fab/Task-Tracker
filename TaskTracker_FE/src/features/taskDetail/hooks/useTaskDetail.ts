import { useQuery } from '@tanstack/react-query'
import { fetchTaskDetail } from '../api/taskDetail.api'

export function useTaskDetail(taskId: number) {
  return useQuery({
    queryKey: ['tasks', 'detail', taskId],
    queryFn: () => fetchTaskDetail(taskId),
    enabled: Number.isFinite(taskId),
  })
}
