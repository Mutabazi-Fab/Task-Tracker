import { useQuery } from '@tanstack/react-query'
import { fetchUnreadCount } from '../api/notifications.api'

/** Polled every 30s so the badge notices new notifications without a manual refresh —
 *  cheap enough at this scale, and simpler than wiring up a push channel for it. */
export function useUnreadCount(personId: number) {
  return useQuery({
    queryKey: ['notifications', 'unread-count', personId],
    queryFn: () => fetchUnreadCount(personId),
    enabled: Number.isFinite(personId),
    refetchInterval: 30_000,
  })
}
