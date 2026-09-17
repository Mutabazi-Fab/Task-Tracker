import { Card } from './Card'
import styles from './StatCard.module.css'

interface StatCardProps {
  label: string
  value: string
  note?: string
  /** Smaller padding/type — for a row with many tiles at once (see the dashboard's two
   *  stacked KPI rows) where the default size takes up too much vertical space. Teams'/
   *  People's own stat rows keep the default size, unchanged. */
  compact?: boolean
}

/** label + big number + note. The building block of every KPI row. */
export function StatCard({ label, value, note, compact }: StatCardProps) {
  return (
    <Card padding={compact ? 'xs' : 'md'}>
      <div className={styles.label}>{label}</div>
      <div className={[styles.value, compact && styles.valueCompact].filter(Boolean).join(' ')}>{value}</div>
      {note && <div className={[styles.note, compact && styles.noteCompact].filter(Boolean).join(' ')}>{note}</div>}
    </Card>
  )
}
