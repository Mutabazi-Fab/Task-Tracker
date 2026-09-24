import type { IncidentStatus } from '../../../types/incident.types'
import { statusLabel } from '../lib/incidentLabels'
import styles from './IncidentStatusBadge.module.css'

const STYLE: Record<IncidentStatus, string> = {
  OPEN: styles.open,
  UNDER_INVESTIGATION: styles.investigating,
  MONITORING: styles.monitoring,
  CLOSED: styles.closed,
  REJECTED_NOT_AN_INCIDENT: styles.rejected,
}

export function IncidentStatusBadge({ status }: { status: IncidentStatus }) {
  return <span className={[styles.chip, STYLE[status]].join(' ')}>{statusLabel(status)}</span>
}
