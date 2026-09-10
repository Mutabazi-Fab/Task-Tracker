import { useMutation, useQueryClient } from '@tanstack/react-query'
import { extendDeadlineDirectly } from '../api/taskDetail.api'

export function useExtendDeadlineDirectly(taskId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: Parameters<typeof extendDeadlineDirectly>[1]) => extendDeadlineDirectly(taskId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
    },
  })
}
