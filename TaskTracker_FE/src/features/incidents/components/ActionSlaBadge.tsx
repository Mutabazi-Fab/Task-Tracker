import type { ActionSlaStatus } from '../../../types/incident.types'
import { actionSlaLabel } from '../lib/incidentLabels'
import styles from './ActionSlaBadge.module.css'

const STYLE: Record<ActionSlaStatus, string> = {
  ON_TRACK: styles.onTrack,
  DUE_SOON: styles.dueSoon,
  OVERDUE: styles.overdue,
  NO_DUE_DATE: styles.noDueDate,
  CLOSED: styles.closed,
  CLOSED_LATE: styles.closedLate,
}

export function ActionSlaBadge({ sla }: { sla: ActionSlaStatus }) {
  return <span className={[styles.chip, STYLE[sla]].join(' ')}>{actionSlaLabel(sla)}</span>
}
