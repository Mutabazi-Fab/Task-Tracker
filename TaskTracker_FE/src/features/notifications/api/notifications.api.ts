import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { Page } from '../../../types/task.types'
import type { Notification, NotificationType } from '../../../types/notification.types'

/** Flattened, same trade-off as elsewhere — this is a compact dropdown list, not a paged
 *  view (yet). */
export async function fetchNotifications(personId: number): Promise<Notification[]> {
  const { data } = await axiosClient.get<Page<Notification>>(endpoints.notifications.list(), {
    params: { personId, size: 20, sort: 'createdAt,desc' },
  })
  return data.content
}

export async function fetchUnreadCount(personId: number): Promise<number> {
  const { data } = await axiosClient.get<number>(endpoints.notifications.unreadCount(), {
    params: { personId },
  })
  return data
}

export async function markNotificationRead(id: number, personId: number): Promise<Notification> {
  const { data } = await axiosClient.put<Notification>(endpoints.notifications.markRead(id), null, {
    params: { personId },
  })
  return data
}

/** Backs the sidebar's per-section badges — one call covering every NotificationType's
 *  unread count at once. A type with zero unread simply isn't a key in the response. */
export async function fetchUnreadCountsByType(): Promise<Partial<Record<NotificationType, number>>> {
  const { data } = await axiosClient.get<Partial<Record<NotificationType, number>>>(
    endpoints.notifications.unreadCountsByType(),
  )
  return data
}

/** Called when the viewer opens the page a badge points at, to clear it. */
export async function markCategoryRead(types: NotificationType[]): Promise<void> {
  await axiosClient.put(endpoints.notifications.markCategoryRead(), null, { params: { types: types.join(',') } })
}
