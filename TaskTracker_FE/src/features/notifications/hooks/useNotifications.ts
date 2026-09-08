import { useQuery } from '@tanstack/react-query'
import { fetchNotifications } from '../api/notifications.api'

/** Polled every 30s, same interval as useUnreadCount — without this, the badge count
 *  updated live but the dropdown's actual list sat on whatever was fetched at mount until
 *  something else happened to invalidate it (e.g. marking one read), so a new notification
 *  could bump the badge to "1" while the panel underneath still showed "Nothing yet". */
export function useNotifications(personId: number) {
  return useQuery({
    queryKey: ['notifications', 'list', personId],
    queryFn: () => fetchNotifications(personId),
    enabled: Number.isFinite(personId),
    refetchInterval: 30_000,
  })
}
