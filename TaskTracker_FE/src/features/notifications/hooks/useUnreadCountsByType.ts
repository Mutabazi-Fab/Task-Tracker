import { useQuery } from '@tanstack/react-query'
import { fetchUnreadCountsByType } from '../api/notifications.api'

/** Backs the sidebar's per-section badges (Teams/Departments/Activity). Same 30s poll as
 *  useUnreadCount, for the same reason — cheap at this scale, simpler than a push channel. */
export function useUnreadCountsByType(enabled: boolean) {
  return useQuery({
    queryKey: ['notifications', 'unread-counts-by-type'],
    queryFn: fetchUnreadCountsByType,
    enabled,
    refetchInterval: 30_000,
  })
}
