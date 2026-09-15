import { useMutation, useQueryClient } from '@tanstack/react-query'
import { markCategoryRead } from '../api/notifications.api'

/** Fired when the viewer opens the page a sidebar badge points at (Teams/Departments/
 *  Activity), so that badge clears the same moment the list itself becomes visible. */
export function useMarkCategoryRead() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: markCategoryRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })
}
