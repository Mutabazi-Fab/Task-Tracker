import { useMutation, useQueryClient } from '@tanstack/react-query'
import { setPinned } from '../api/taskDetail.api'

/** Pinned-first ordering is composed server-side onto every task list, so pinning here
 *  can change where this task sorts org-wide, not just its own detail view. */
export function useSetPinned(taskId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: Parameters<typeof setPinned>[1]) => setPinned(taskId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}
