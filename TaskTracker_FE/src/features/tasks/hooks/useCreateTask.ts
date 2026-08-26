import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createTask } from '../api/tasks.api'

/**
 * Mutation + cache invalidation. A new task changes org-wide numbers, so
 * dashboard/people/teams queries are invalidated alongside the task list.
 */
export function useCreateTask() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createTask,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['people'] })
      queryClient.invalidateQueries({ queryKey: ['teams'] })
    },
  })
}
