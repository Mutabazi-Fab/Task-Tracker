import { useMutation, useQueryClient } from '@tanstack/react-query'
import { decideDeadlineExtension } from '../api/taskDetail.api'

export function useDecideDeadlineExtension(taskId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ extensionId, ...payload }: { extensionId: number } & Parameters<typeof decideDeadlineExtension>[2]) =>
      decideDeadlineExtension(taskId, extensionId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      // A decision made here (from a task's own history panel) also clears that request
      // out of the dashboard's "Requests" inbox, and one decided from the inbox itself
      // (see PendingExtensionRequestItem, which reuses this same hook) needs this task's
      // own detail/history refetched too — 'tasks' above already covers that half.
      queryClient.invalidateQueries({ queryKey: ['pendingExtensionRequests'] })
    },
  })
}
