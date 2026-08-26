import { Card } from './Card'
import styles from './StatCard.module.css'

interface StatCardProps {
  label: string
  value: string
  note?: string
}

/** label + big number + note. The building block of every KPI row. */
export function StatCard({ label, value, note }: StatCardProps) {
  return (
    <Card>
      <div className={styles.label}>{label}</div>
      <div className={styles.value}>{value}</div>
      {note && <div className={styles.note}>{note}</div>}
    </Card>
  )
}
