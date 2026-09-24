import { useMemo, useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { usePeople } from '../../people/hooks/usePeople'
import { IncidentSeverityBadge } from './IncidentSeverityBadge'
import { computeInherentScore, computeNetLoss, computeSeverity } from '../lib/riskCalc'
import { formatCurrency } from '../lib/incidentLabels'
import type {
  DataBreachStatus,
  IncidentDetail,
  IncidentReviewStatus,
  RegulatorNotifiableStatus,
  UpdateIncidentRequest,
} from '../../../types/incident.types'
import styles from './CreateIncidentForm.module.css'

const SCORE_OPTIONS = [1, 2, 3, 4, 5].map((n) => ({ label: String(n), value: String(n) }))

const DATA_BREACH_OPTIONS: { label: string; value: DataBreachStatus }[] = [
  { label: 'No', value: 'NO' },
  { label: 'Yes', value: 'YES' },
  { label: 'Potential', value: 'POTENTIAL' },
]

const REGULATOR_OPTIONS: { label: string; value: RegulatorNotifiableStatus }[] = [
  { label: 'No', value: 'NO' },
  { label: 'Yes', value: 'YES' },
  { label: 'Assess', value: 'ASSESS' },
]

const REVIEW_OPTIONS: { label: string; value: IncidentReviewStatus }[] = [
  { label: 'Pending', value: 'PENDING' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Not required', value: 'NOT_REQUIRED' },
]

interface EditIncidentFormProps {
  incident: IncidentDetail
  onSubmit: (payload: Omit<UpdateIncidentRequest, 'changedById'>) => void
  onCancel: () => void
  submitting: boolean
}

/** Edits everything the investigation/closure lifecycle needs (see UpdateIncidentRequest) —
 *  status itself is changed separately (see ChangeIncidentStatusModal), which is also what
 *  enforces closure readiness. Same live Inherent Score/Severity/Net Loss preview as
 *  CreateIncidentForm. */
export function EditIncidentForm({ incident, onSubmit, onCancel, submitting }: EditIncidentFormProps) {
  const peopleQuery = usePeople()

  const [title, setTitle] = useState(incident.title)
  const [description, setDescription] = useState(incident.description ?? '')
  const [locationChannel, setLocationChannel] = useState(incident.locationChannel ?? '')
  const [eventType, setEventType] = useState(incident.eventType ?? '')
  const [likelihood, setLikelihood] = useState(incident.likelihood != null ? String(incident.likelihood) : '')
  const [impact, setImpact] = useState(incident.impact != null ? String(incident.impact) : '')
  const [customersAffected, setCustomersAffected] = useState(incident.customersAffected != null ? String(incident.customersAffected) : '')
  const [serviceDowntimeMinutes, setServiceDowntimeMinutes] = useState(
    incident.serviceDowntimeMinutes != null ? String(incident.serviceDowntimeMinutes) : '',
  )
  const [grossLoss, setGrossLoss] = useState(String(incident.grossLoss))
  const [recovery, setRecovery] = useState(String(incident.recovery))
  const [dataBreach, setDataBreach] = useState<DataBreachStatus>(incident.dataBreach)
  const [regulatorNotifiable, setRegulatorNotifiable] = useState<RegulatorNotifiableStatus>(incident.regulatorNotifiable)
  const [immediateContainment, setImmediateContainment] = useState(incident.immediateContainment ?? '')
  const [rootCause, setRootCause] = useState(incident.rootCause ?? '')
  const [correctiveAction, setCorrectiveAction] = useState(incident.correctiveAction ?? '')
  const [actionOwnerId, setActionOwnerId] = useState(incident.actionOwnerId != null ? String(incident.actionOwnerId) : '')
  const [targetClosureDate, setTargetClosureDate] = useState(incident.targetClosureDate ?? '')
  const [actualClosureDate, setActualClosureDate] = useState(incident.actualClosureDate ?? '')
  const [evidenceReference, setEvidenceReference] = useState(incident.evidenceReference ?? '')
  const [incidentOwner, setIncidentOwner] = useState(incident.incidentOwner ?? '')
  const [riskReview, setRiskReview] = useState<IncidentReviewStatus>(incident.riskReview)
  const [complianceReview, setComplianceReview] = useState<IncidentReviewStatus>(incident.complianceReview)
  const [lessonsLearned, setLessonsLearned] = useState(incident.lessonsLearned ?? '')

  const inherentScore = useMemo(
    () => computeInherentScore(likelihood ? Number(likelihood) : null, impact ? Number(impact) : null),
    [likelihood, impact],
  )
  const severity = useMemo(() => computeSeverity(inherentScore), [inherentScore])
  const netLoss = useMemo(
    () => computeNetLoss(grossLoss ? Number(grossLoss) : 0, recovery ? Number(recovery) : 0),
    [grossLoss, recovery],
  )

  const isValid = title.trim() !== ''

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid) return

    onSubmit({
      title: title.trim(),
      description: description.trim() || undefined,
      locationChannel: locationChannel.trim() || undefined,
      eventType: eventType.trim() || undefined,
      likelihood: likelihood ? Number(likelihood) : undefined,
      impact: impact ? Number(impact) : undefined,
      customersAffected: customersAffected ? Number(customersAffected) : undefined,
      serviceDowntimeMinutes: serviceDowntimeMinutes ? Number(serviceDowntimeMinutes) : undefined,
      grossLoss: grossLoss ? Number(grossLoss) : undefined,
      recovery: recovery ? Number(recovery) : undefined,
      dataBreach,
      regulatorNotifiable,
      immediateContainment: immediateContainment.trim() || undefined,
      rootCause: rootCause.trim() || undefined,
      correctiveAction: correctiveAction.trim() || undefined,
      actionOwnerId: actionOwnerId ? Number(actionOwnerId) : undefined,
      targetClosureDate: targetClosureDate || undefined,
      actualClosureDate: actualClosureDate || undefined,
      evidenceReference: evidenceReference.trim() || undefined,
      incidentOwner: incidentOwner.trim() || undefined,
      riskReview,
      complianceReview,
      lessonsLearned: lessonsLearned.trim() || undefined,
    })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <TextField label="Title" value={title} onChange={setTitle} required />
      <TextField label="Description" value={description} onChange={setDescription} />

      <div className={styles.row}>
        <TextField label="Location / channel" value={locationChannel} onChange={setLocationChannel} />
        <TextField label="Event type" value={eventType} onChange={setEventType} />
      </div>

      <div className={styles.riskBlock}>
        <div className={styles.row}>
          <SelectField label="Likelihood (1-5)" value={likelihood} onChange={setLikelihood} placeholder="Not yet scored" options={SCORE_OPTIONS} />
          <SelectField label="Impact (1-5)" value={impact} onChange={setImpact} placeholder="Not yet scored" options={SCORE_OPTIONS} />
        </div>
        <div className={styles.riskPreview}>
          <span className={styles.riskLabel}>Inherent score: {inherentScore ?? '—'}</span>
          <IncidentSeverityBadge severity={severity} />
        </div>
      </div>

      <div className={styles.row}>
        <TextField label="Customers affected" type="number" min="0" value={customersAffected} onChange={setCustomersAffected} />
        <TextField label="Service downtime (minutes)" type="number" min="0" value={serviceDowntimeMinutes} onChange={setServiceDowntimeMinutes} />
      </div>

      <div className={styles.lossBlock}>
        <div className={styles.row}>
          <TextField label="Gross loss (RWF)" type="number" min="0" value={grossLoss} onChange={setGrossLoss} />
          <TextField label="Recovery (RWF)" type="number" min="0" value={recovery} onChange={setRecovery} />
        </div>
        <span className={styles.netLoss}>Net loss: {formatCurrency(netLoss)}</span>
      </div>

      <div className={styles.row}>
        <SelectField label="Data / privacy breach?" value={dataBreach} onChange={(v) => setDataBreach(v as DataBreachStatus)} options={DATA_BREACH_OPTIONS} />
        <SelectField
          label="Regulator notifiable?"
          value={regulatorNotifiable}
          onChange={(v) => setRegulatorNotifiable(v as RegulatorNotifiableStatus)}
          options={REGULATOR_OPTIONS}
        />
      </div>

      <TextField label="Immediate containment" value={immediateContainment} onChange={setImmediateContainment} />
      <TextField label="Root cause" value={rootCause} onChange={setRootCause} placeholder="Required before closing" />
      <TextField label="Corrective / preventive action" value={correctiveAction} onChange={setCorrectiveAction} placeholder="Required before closing" />

      <div className={styles.row}>
        <SelectField
          label="Action owner"
          value={actionOwnerId}
          onChange={setActionOwnerId}
          placeholder={peopleQuery.isLoading ? 'Loading…' : 'Not yet assigned'}
          options={(peopleQuery.data ?? []).map((p) => ({ label: p.fullName, value: String(p.id) }))}
        />
        <TextField label="Incident owner" value={incidentOwner} onChange={setIncidentOwner} placeholder="Free text" />
      </div>

      <div className={styles.row}>
        <TextField label="Target closure date" type="date" value={targetClosureDate} onChange={setTargetClosureDate} />
        <TextField label="Actual closure date" type="date" value={actualClosureDate} onChange={setActualClosureDate} />
      </div>

      <div className={styles.row}>
        <SelectField label="Risk review" value={riskReview} onChange={(v) => setRiskReview(v as IncidentReviewStatus)} options={REVIEW_OPTIONS} />
        <SelectField
          label="Compliance review"
          value={complianceReview}
          onChange={(v) => setComplianceReview(v as IncidentReviewStatus)}
          options={REVIEW_OPTIONS}
        />
      </div>

      <TextField label="Evidence / reference" value={evidenceReference} onChange={setEvidenceReference} />
      <TextField label="Lessons learned" value={lessonsLearned} onChange={setLessonsLearned} />

      <div className={styles.actions}>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" variant="primary" disabled={!isValid || submitting}>
          {submitting ? 'Saving…' : 'Save changes'}
        </Button>
      </div>
    </form>
  )
}
