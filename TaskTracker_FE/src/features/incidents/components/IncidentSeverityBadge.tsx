import type { IncidentSeverity } from '../../../types/incident.types'
import styles from './IncidentSeverityBadge.module.css'

const LABEL: Record<IncidentSeverity, string> = {
  LOW: 'Low',
  MODERATE: 'Moderate',
  HIGH: 'High',
  CRITICAL: 'Critical',
}

const STYLE: Record<IncidentSeverity, string> = {
  LOW: styles.low,
  MODERATE: styles.moderate,
  HIGH: styles.high,
  CRITICAL: styles.critical,
}

/** severity is null until Likelihood and Impact are both scored — shown as a plain "Not yet
 *  scored" pill instead of the source Excel's literal FALSE-when-blank bug. */
export function IncidentSeverityBadge({ severity }: { severity: IncidentSeverity | null }) {
  if (severity === null) {
    return <span className={`${styles.badge} ${styles.unscored}`}>Not yet scored</span>
  }
  return <span className={`${styles.badge} ${STYLE[severity]}`}>{LABEL[severity]}</span>
}
