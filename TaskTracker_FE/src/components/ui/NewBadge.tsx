import styles from './NewBadge.module.css'

/** Shown on a task's row/card for 24 hours after creation — see isRecentlyCreated. Same
 *  pill shape as SeverityBadge/StatusChip, own accent-green colour so it reads as "fresh"
 *  rather than a status or severity signal. */
export function NewBadge() {
  return <span className={styles.badge}>New</span>
}
