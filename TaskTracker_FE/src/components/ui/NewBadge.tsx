import styles from './NewBadge.module.css'

/** Shown on a task's row/card for 24 hours after creation — see isRecentlyCreated. */
export function NewBadge() {
  return <span className={styles.badge}>New</span>
}
