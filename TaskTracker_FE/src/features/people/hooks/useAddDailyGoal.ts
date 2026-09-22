import { useMutation, useQueryClient } from '@tanstack/react-query'
import { addDailyGoal } from '../api/people.api'

export function useAddDailyGoal(personId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (taskId: number) => addDailyGoal(personId, taskId),
    onSuccess: (stats) => {
      // The response already carries the refreshed dailyGoalTasks list — write it straight
      // into the cache instead of waiting on a refetch.
      queryClient.setQueryData(['people', 'statistics', personId], stats)
    },
  })
}
