import type { DepartmentHealthStatus } from '../../../types/dashboard.types'
import styles from './DepartmentHealthBadge.module.css'

// Deliberately its own mapping, not statusColorKey — DepartmentHealthStatus (ON_TRACK/
// AT_RISK/BEHIND) is a different 3-state scale than TaskStatus, even though it draws from
// the same --status-* CSS custom properties for visual consistency (on-track = completed
// green, at-risk = ongoing amber, behind = pending red).
const HEALTH_CLASS: Record<DepartmentHealthStatus, string> = {
  ON_TRACK: styles.onTrack,
  AT_RISK: styles.atRisk,
  BEHIND: styles.behind,
}
const HEALTH_LABEL: Record<DepartmentHealthStatus, string> = {
  ON_TRACK: 'On track',
  AT_RISK: 'At risk',
  BEHIND: 'Behind',
}

export function DepartmentHealthBadge({ health }: { health: DepartmentHealthStatus }) {
  return <span className={[styles.badge, HEALTH_CLASS[health]].join(' ')}>{HEALTH_LABEL[health]}</span>
}
