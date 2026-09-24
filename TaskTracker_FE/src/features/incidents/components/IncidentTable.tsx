import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routes'
import { EmptyState } from '../../../components/ui/EmptyState'
import { formatDate } from '../../../lib/formatDate'
import type { IncidentListItem } from '../../../types/incident.types'
import { businessUnitLabel, categoryLabel, formatCurrency } from '../lib/incidentLabels'
import { SharedNote } from '../../accessGrants/components/SharedNote'
import { IncidentSeverityBadge } from './IncidentSeverityBadge'
import { IncidentStatusBadge } from './IncidentStatusBadge'
import { ActionSlaBadge } from './ActionSlaBadge'
import styles from './IncidentTable.module.css'

export function IncidentTable({ incidents }: { incidents: IncidentListItem[] }) {
  if (incidents.length === 0) {
    return <EmptyState title="No incidents match this view" description="Try a different filter or search." />
  }

  return (
    <div className={styles.table}>
      <div className={styles.headerRow}>
        <span>Code</span>
        <span>Title</span>
        <span>Business unit</span>
        <span>Severity</span>
        <span>Status</span>
        <span>Days open</span>
        <span>Action SLA</span>
        <span>Owner</span>
      </div>
      {incidents.map((incident) => (
        <Link key={incident.id} to={ROUTES.incidentDetail(incident.id)} className={styles.row}>
          <span className={styles.code}>{incident.incidentCode}</span>
          <div className={styles.title}>
            <span className={styles.titleText}>{incident.title}</span>
            <SharedNote resourceType="INCIDENT" resourceId={incident.id} />
            <span className={styles.subText}>
              {categoryLabel(incident.category)} · {formatDate(incident.dateOccurred)}
            </span>
          </div>
          <span className={styles.subText}>{businessUnitLabel(incident.businessUnit)}</span>
          <IncidentSeverityBadge severity={incident.severity} />
          <IncidentStatusBadge status={incident.status} />
          <span className={styles.netLoss}>{incident.daysOpen}d</span>
          <ActionSlaBadge sla={incident.actionSla} />
          <div className={styles.owner}>
            {incident.actionOwnerName ? (
              <span className={styles.ownerName}>{incident.actionOwnerName}</span>
            ) : (
              <span className={styles.unassigned}>Unassigned</span>
            )}
          </div>
        </Link>
      ))}
    </div>
  )
}
