import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteTask } from '../../tasks/api/tasks.api'

/** Deleting a subtask changes its parent's rolled-up percentage, so the same broad
 *  invalidation as everywhere else that moves a task's numbers. */
export function useDeleteTask() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => deleteTask(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['people'] })
      queryClient.invalidateQueries({ queryKey: ['teams'] })
    },
  })
}
