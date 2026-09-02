import styles from './ErrorMessage.module.css'

/**
 * Failure callout — light red (--status-pending), the same danger colour used for
 * "Deactivate account" and the task Delete button, so a real error reads as one.
 */
export function ErrorMessage({ message }: { message: string }) {
  return (
    <div className={styles.wrap} role="alert">
      <span className={styles.label}>Error</span>
      <span className={styles.message}>{message}</span>
    </div>
  )
}
