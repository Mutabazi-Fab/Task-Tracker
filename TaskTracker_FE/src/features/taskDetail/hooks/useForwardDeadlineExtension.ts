import { useMutation, useQueryClient } from '@tanstack/react-query'
import { forwardDeadlineExtension } from '../api/taskDetail.api'

export function useForwardDeadlineExtension(taskId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (extensionId: number) => forwardDeadlineExtension(taskId, extensionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      // Same reasoning as useDecideDeadlineExtension's own invalidation: this can be
      // triggered from the dashboard's "Requests" inbox (see PendingExtensionRequestItem),
      // and forwarding doesn't remove the row from the Director's own inbox, but it does
      // change what that row looks like (forwardedToApprover flips) and adds it to the
      // CEO's inbox for the first time.
      queryClient.invalidateQueries({ queryKey: ['pendingExtensionRequests'] })
    },
  })
}
