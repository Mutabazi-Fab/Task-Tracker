import { useMutation, useQueryClient } from '@tanstack/react-query'
import { addDiscussionComment } from '../api/taskDetail.api'

/** Never changes percentage/status — only this task's own detail needs invalidating, not
 *  the broader dashboard/people/teams numbers the way a real progress update does. */
export function useAddDiscussionComment(taskId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: Parameters<typeof addDiscussionComment>[1]) => addDiscussionComment(taskId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', 'detail', taskId] })
    },
  })
}
