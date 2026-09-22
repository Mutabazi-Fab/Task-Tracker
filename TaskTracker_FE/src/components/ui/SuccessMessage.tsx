import styles from './SuccessMessage.module.css'

/** Success callout — the mirror image of ErrorMessage, in the app's green
 *  (--status-completed) instead of its red, so a completed action reads as one. */
export function SuccessMessage({ message }: { message: string }) {
  return (
    <div className={styles.wrap} role="status">
      <span className={styles.label}>Done</span>
      <span className={styles.message}>{message}</span>
    </div>
  )
}
