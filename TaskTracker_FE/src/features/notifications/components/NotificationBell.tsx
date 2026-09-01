import { useEffect, useRef, useState } from 'react'
import { Icon } from '../../../components/ui/Icon'
import { formatDateTime } from '../../../lib/formatDate'
import { useAuth } from '../../auth/useAuth'
import { useMarkNotificationRead } from '../hooks/useMarkNotificationRead'
import { useNotifications } from '../hooks/useNotifications'
import { useUnreadCount } from '../hooks/useUnreadCount'
import styles from './NotificationBell.module.css'

/**
 * Lives in AppShell's top bar, next to global search, so it's on every page — same
 * reasoning as SearchInput. Click toggles a small dropdown rather than a full page;
 * clicking a notification marks it read in place.
 */
export function NotificationBell() {
  const { currentUser } = useAuth()
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
            notifications.map((n) => (
              <button
                key={n.id}
                type="button"
                className={`${styles.item} ${!n.isRead ? styles.itemUnread : ''}`}
                onClick={() => !n.isRead && markRead.mutate(n.id)}
              >
                {n.message}
                <span className={styles.timestamp}>{formatDateTime(n.createdAt)}</span>
              </button>
            ))
          )}
        </div>
      )}
    </div>
  )
}
