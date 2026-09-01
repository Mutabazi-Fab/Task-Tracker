import { useMutation, useQueryClient } from '@tanstack/react-query'
import { markNotificationRead } from '../api/notifications.api'

export function useMarkNotificationRead(personId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => markNotificationRead(id, personId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })
}
