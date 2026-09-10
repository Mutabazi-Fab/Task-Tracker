import { useMutation, useQueryClient } from '@tanstack/react-query'
import { decideDeadlineExtension } from '../api/taskDetail.api'

export function useDecideDeadlineExtension(taskId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ extensionId, ...payload }: { extensionId: number } & Parameters<typeof decideDeadlineExtension>[2]) =>
      decideDeadlineExtension(taskId, extensionId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
    },
  })
}
