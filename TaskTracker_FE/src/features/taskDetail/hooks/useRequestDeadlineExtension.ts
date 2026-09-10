import { useMutation, useQueryClient } from '@tanstack/react-query'
import { requestDeadlineExtension } from '../api/taskDetail.api'

export function useRequestDeadlineExtension(taskId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: Parameters<typeof requestDeadlineExtension>[1]) =>
      requestDeadlineExtension(taskId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
    },
  })
}
