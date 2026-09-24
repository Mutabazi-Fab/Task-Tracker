import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { formatDate, formatDateTime } from '../../lib/formatDate'
import { useAuth } from '../auth/useAuth'
import { useIncident } from './hooks/useIncident'
import { IncidentSeverityBadge } from './components/IncidentSeverityBadge'
import { IncidentStatusBadge } from './components/IncidentStatusBadge'
import { ActionSlaBadge } from './components/ActionSlaBadge'
import { ChangeStatusPanel } from './components/ChangeStatusPanel'
import { EditIncidentModal } from './components/EditIncidentModal'
import {
  businessUnitLabel,
  categoryLabel,
  dataBreachLabel,
  formatCurrency,
  regulatorNotifiableLabel,
  reviewStatusLabel,
  statusLabel,
} from './lib/incidentLabels'
import type { IncidentDetail } from '../../types/incident.types'
import styles from './IncidentDetailPage.module.css'

function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className={styles.fieldRow}>
      <span className={styles.fieldLabel}>{label}</span>
      <span className={styles.fieldValue}>{value}</span>
    </div>
  )
}

function IncidentDetailBody({ incident }: { incident: IncidentDetail }) {
  const { isDirector } = useAuth()
  const [editOpen, setEditOpen] = useState(false)
  const navigate = useNavigate()

  return (
    <>
      <PageHeader
        breadcrumb="Throughline / Incident Management"
        title={`${incident.incidentCode} — ${incident.title}`}
        onBack={() => navigate(-1)}
        right={isDirector ? <Button onClick={() => setEditOpen(true)}>Edit</Button> : undefined}
      />

      <div className={styles.badgeRow}>
        <IncidentStatusBadge status={incident.status} />
        <IncidentSeverityBadge severity={incident.severity} />
        <ActionSlaBadge sla={incident.actionSla} />
      </div>

      {isDirector && (
        <Card>
          <div className={styles.sectionHeadingLg}>Change status</div>
          <ChangeStatusPanel incident={incident} />
        </Card>
      )}

      <div className={styles.grid}>
        <Card>
          <div className={styles.sectionHeadingLg}>Incident details</div>
          <div className={styles.fieldList}>
            <Field label="Date occurred" value={formatDate(incident.dateOccurred)} />
            <Field label="Date discovered" value={formatDate(incident.dateDiscovered)} />
            <Field label="Date reported" value={formatDate(incident.dateReported)} />
            <Field label="Business unit" value={businessUnitLabel(incident.businessUnit)} />
            <Field label="Location / channel" value={incident.locationChannel ?? '—'} />
            <Field label="Category" value={categoryLabel(incident.category)} />
            <Field label="Event type" value={incident.eventType ?? '—'} />
            <Field label="Description" value={incident.description ?? '—'} />
            <Field label="Reported by" value={incident.reportedByName} />
            <Field label="Incident owner" value={incident.incidentOwner ?? '—'} />
          </div>
        </Card>

        <Card>
          <div className={styles.sectionHeadingLg}>Risk & impact</div>
          <div className={styles.fieldList}>
            <Field label="Likelihood" value={incident.likelihood ?? '—'} />
            <Field label="Impact" value={incident.impact ?? '—'} />
            <Field label="Inherent score" value={incident.inherentScore ?? '—'} />
            <Field label="Customers affected" value={incident.customersAffected ?? '—'} />
            <Field label="Service downtime (min)" value={incident.serviceDowntimeMinutes ?? '—'} />
            <Field label="Gross loss" value={formatCurrency(incident.grossLoss)} />
            <Field label="Recovery" value={formatCurrency(incident.recovery)} />
            <Field label="Net loss" value={formatCurrency(incident.netLoss)} />
            <Field label="Data / privacy breach" value={dataBreachLabel(incident.dataBreach)} />
            <Field label="Regulator notifiable" value={regulatorNotifiableLabel(incident.regulatorNotifiable)} />
            {incident.bnrNotificationDate && <Field label="BNR notification date" value={formatDate(incident.bnrNotificationDate)} />}
          </div>
        </Card>

        <Card>
          <div className={styles.sectionHeadingLg}>Investigation & closure</div>
          <div className={styles.fieldList}>
            <Field label="Immediate containment" value={incident.immediateContainment ?? '—'} />
            <Field label="Root cause" value={incident.rootCause ?? '—'} />
            <Field label="Corrective / preventive action" value={incident.correctiveAction ?? '—'} />
            <Field label="Action owner" value={incident.actionOwnerName ?? 'Unassigned'} />
            <Field label="Target closure date" value={incident.targetClosureDate ? formatDate(incident.targetClosureDate) : '—'} />
            <Field label="Actual closure date" value={incident.actualClosureDate ? formatDate(incident.actualClosureDate) : '—'} />
            <Field label="Days open" value={incident.daysOpen} />
            <Field label="Evidence / reference" value={incident.evidenceReference ?? '—'} />
            <Field label="Risk review" value={reviewStatusLabel(incident.riskReview)} />
            <Field label="Compliance review" value={reviewStatusLabel(incident.complianceReview)} />
            <Field label="Lessons learned" value={incident.lessonsLearned ?? '—'} />
          </div>
        </Card>

        <Card>
          <div className={styles.sectionHeadingLg}>Status history</div>
          {incident.statusHistory.length === 0 ? (
            <p>No changes yet.</p>
          ) : (
            <div>
              {incident.statusHistory.map((change) => (
                <div key={change.id} className={styles.historyItem}>
                  <span>
                    {change.fromStatus ? `${statusLabel(change.fromStatus)} → ${statusLabel(change.toStatus)}` : statusLabel(change.toStatus)}
                  </span>
                  <span className={styles.historyMeta}>
                    {change.changedByName} · {formatDateTime(change.changedAt)}
                    {change.note ? ` · ${change.note}` : ''}
                  </span>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      {isDirector && <EditIncidentModal incident={incident} open={editOpen} onClose={() => setEditOpen(false)} />}
    </>
  )
}

export function IncidentDetailPage() {
  const { incidentId } = useParams<{ incidentId: string }>()
  const query = useIncident(Number(incidentId))

  return <QueryBoundary query={query}>{(incident) => <IncidentDetailBody incident={incident} />}</QueryBoundary>
}
