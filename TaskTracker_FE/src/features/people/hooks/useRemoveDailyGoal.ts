import { useMutation, useQueryClient } from '@tanstack/react-query'
import { removeDailyGoal } from '../api/people.api'

export function useRemoveDailyGoal(personId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (taskId: number) => removeDailyGoal(personId, taskId),
    onSuccess: (stats) => {
      queryClient.setQueryData(['people', 'statistics', personId], stats)
    },
  })
}
