import styles from './ProgressChartLegend.module.css'

export function ProgressChartLegend() {
  return (
    <div className={styles.legend}>
      <span className={styles.swatch} />
      <span className={styles.label}>Org-wide average progress — trailing 30 days</span>
    </div>
  )
}
