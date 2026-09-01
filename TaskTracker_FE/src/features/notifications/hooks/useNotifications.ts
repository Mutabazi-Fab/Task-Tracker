import { useQuery } from '@tanstack/react-query'
import { fetchNotifications } from '../api/notifications.api'

export function useNotifications(personId: number) {
  return useQuery({
    queryKey: ['notifications', 'list', personId],
    queryFn: () => fetchNotifications(personId),
    enabled: Number.isFinite(personId),
  })
}
