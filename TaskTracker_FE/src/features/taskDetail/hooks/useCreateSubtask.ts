import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createSubtask } from '../../tasks/api/tasks.api'

/** A new subtask changes its parent's rolled-up percentage, so the parent's own detail
 *  query is invalidated too, not just the task list / org-wide numbers. */
export function useCreateSubtask(parentTaskId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: Parameters<typeof createSubtask>[1]) => createSubtask(parentTaskId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['people'] })
      queryClient.invalidateQueries({ queryKey: ['teams'] })
    },
  })
}
