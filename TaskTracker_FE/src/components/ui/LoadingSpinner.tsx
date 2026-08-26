import styles from './LoadingSpinner.module.css'

/** Restrained, operational spinner — a rotating bracket, no illustration. */
export function LoadingSpinner({ label = 'Loading' }: { label?: string }) {
  return (
    <div className={styles.wrap} role="status" aria-live="polite">
      <span className={styles.spinner} />
      <span className={styles.label}>{label}</span>
    </div>
  )
}
