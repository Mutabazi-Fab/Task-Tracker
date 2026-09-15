import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Icon } from '../../../components/ui/Icon'
import { formatDateTime } from '../../../lib/formatDate'
import { ROUTES } from '../../../app/routePaths'
import { useAuth } from '../../auth/useAuth'
import { useMarkNotificationRead } from '../hooks/useMarkNotificationRead'
import { useNotifications } from '../hooks/useNotifications'
import { useUnreadCount } from '../hooks/useUnreadCount'
import type { Notification, NotificationType } from '../../../types/notification.types'
import styles from './NotificationBell.module.css'

// Every task-shaped notification carries the task's own id as relatedEntityId (see
// NotificationServiceImpl's various notify* methods — they all pass task.getId() or
// subtask.getId()). ROLE_CHANGED/ACCOUNT_STATUS_CHANGED/PASSWORD_RESET_REQUESTED instead
// carry the affected person's id. TEAM_MEMBER_ADDED/REMOVED/TEAM_LEADER_CHANGED carry a
// TeamMembershipChange record's own id, not a team id — nowhere to send someone yet, so
// those stay non-navigable (still mark read on click, same as before).
const TASK_NOTIFICATION_TYPES = new Set<NotificationType>([
  'TASK_STALLED',
  'TASK_ASSIGNED',
  'SUBTASK_ASSIGNED',
  'TASK_REASSIGNED',
  'SUBTASK_REASSIGNED',
  'DEADLINE_EXTENSION_REQUESTED',
  'DEADLINE_EXTENSION_APPROVED',
  'DEADLINE_EXTENSION_REJECTED',
  'DEADLINE_EXTENDED',
  'DISCUSSION_COMMENT_POSTED',
  'DISCUSSION_REPLY_POSTED',
])
const PERSON_NOTIFICATION_TYPES = new Set<NotificationType>([
  'ROLE_CHANGED',
  'ACCOUNT_STATUS_CHANGED',
  'PASSWORD_RESET_REQUESTED',
])

function resolveNotificationRoute(notification: Notification): string | null {
  if (notification.relatedEntityId == null) return null
  if (TASK_NOTIFICATION_TYPES.has(notification.type)) return ROUTES.taskDetail(notification.relatedEntityId)
  if (PERSON_NOTIFICATION_TYPES.has(notification.type)) return ROUTES.personProfile(notification.relatedEntityId)
  return null
}

/**
 * Lives in AppShell's top bar, next to global search, so it's on every page — same
 * reasoning as SearchInput. Click toggles a small dropdown rather than a full page;
 * clicking a notification marks it read in place AND, when it points at something real
 * (a task, or a person for a role/account-status one), navigates straight there — no more
 * reading "Solange requested an extension on TSK-0019" and then hunting for that task by
 * hand.
 */
export function NotificationBell() {
  const { currentUser } = useAuth()
  const navigate = useNavigate()
  const personId = currentUser?.id ?? NaN
  const [open, setOpen] = useState(false)
  const wrapRef = useRef<HTMLDivElement>(null)

  const unreadQuery = useUnreadCount(personId)
  const notificationsQuery = useNotifications(personId)
  const markRead = useMarkNotificationRead(personId)

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    if (open) document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [open])

  if (!currentUser) return null

  const unreadCount = unreadQuery.data ?? 0
  const notifications = notificationsQuery.data ?? []

  return (
    <div className={styles.wrap} ref={wrapRef}>
      <button
        type="button"
        className={styles.button}
        onClick={() => setOpen((v) => !v)}
        aria-label="Notifications"
      >
        <Icon name="bell" size={18} />
        {unreadCount > 0 && <span className={styles.badge}>{unreadCount > 99 ? '99+' : unreadCount}</span>}
      </button>

      {open && (
        <div className={styles.panel}>
          <div className={styles.panelHeader}>Notifications</div>
          {notifications.length === 0 ? (
            <div className={styles.empty}>Nothing yet</div>
          ) : (
            notifications.map((n) => {
              const targetRoute = resolveNotificationRoute(n)
              return (
                <button
                  key={n.id}
                  type="button"
                  className={`${styles.item} ${!n.isRead ? styles.itemUnread : ''}`}
                  onClick={() => {
                    if (!n.isRead) markRead.mutate(n.id)
                    if (targetRoute) {
                      setOpen(false)
                      navigate(targetRoute)
                    }
                  }}
                >
                  {n.message}
                  <span className={styles.timestamp}>{formatDateTime(n.createdAt)}</span>
                </button>
              )
            })
          )}
        </div>
      )}
    </div>
  )
}
