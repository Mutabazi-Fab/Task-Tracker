import { useMutation, useQueryClient } from '@tanstack/react-query'
import { extendDeadlineDirectly } from '../api/taskDetail.api'

export function useExtendDeadlineDirectly(taskId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: Parameters<typeof extendDeadlineDirectly>[1]) => extendDeadlineDirectly(taskId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      // Same reasoning as useDecideDeadlineExtension: this moves the task's own deadline,
      // which the Executive dashboard's KPI tiles/department-health roll-up derive from
      // under a separate top-level query key.
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}
