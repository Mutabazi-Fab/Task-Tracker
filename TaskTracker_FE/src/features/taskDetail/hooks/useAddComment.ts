import { useMutation, useQueryClient } from '@tanstack/react-query'
import { addComment } from '../api/taskDetail.api'

/**
 * The ONLY mutation that changes a task's progress. A comment can move
 * org-wide numbers, so dashboard/people/teams are invalidated too, not just
 * this task.
 */
export function useAddComment(taskId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: Parameters<typeof addComment>[1]) => addComment(taskId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['people'] })
      queryClient.invalidateQueries({ queryKey: ['teams'] })
    },
  })
}
