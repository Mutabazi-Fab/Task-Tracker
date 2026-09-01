import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { Page } from '../../../types/task.types'
import type { Notification } from '../../../types/notification.types'

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
