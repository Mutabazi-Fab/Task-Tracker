import styles from './ErrorMessage.module.css'

/**
 * Neutral failure callout. Deliberately does not reach for --status-pending —
 * that red is reserved for task status, not for "the request failed".
 */
export function ErrorMessage({ message }: { message: string }) {
  return (
    <div className={styles.wrap} role="alert">
      <span className={styles.label}>Error</span>
      <span className={styles.message}>{message}</span>
    </div>
  )
}
