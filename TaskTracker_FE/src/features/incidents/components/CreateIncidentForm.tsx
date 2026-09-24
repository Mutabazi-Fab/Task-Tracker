import { useMemo, useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { usePeople } from '../../people/hooks/usePeople'
import { useAuth } from '../../auth/useAuth'
import { IncidentSeverityBadge } from './IncidentSeverityBadge'
import { computeInherentScore, computeNetLoss, computeSeverity } from '../lib/riskCalc'
import { categoryLabel, formatCurrency } from '../lib/incidentLabels'
import { useBusinessUnitOptions } from '../hooks/useBusinessUnitOptions'
import { todayDate } from '../../../lib/dateLimits'
import type {
  CreateIncidentRequest,
  DataBreachStatus,
  IncidentBusinessUnit,
  IncidentCategory,
  RegulatorNotifiableStatus,
} from '../../../types/incident.types'
import styles from './CreateIncidentForm.module.css'

const CATEGORIES: IncidentCategory[] = [
  'INTERNAL_FRAUD', 'EXTERNAL_FRAUD', 'EMPLOYMENT_PRACTICES_SAFETY', 'CLIENTS_PRODUCTS_BUSINESS_PRACTICES',
  'PHYSICAL_ASSET_DAMAGE', 'ICT_SYSTEMS', 'CYBERSECURITY', 'PROCESS_EXECUTION', 'BUSINESS_DISRUPTION_BCM',
  'COMPLIANCE_LEGAL', 'THIRD_PARTY_OUTSOURCING',
]

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

interface CreateIncidentFormProps {
  onSubmit: (payload: CreateIncidentRequest) => void
  onCancel: () => void
  submitting: boolean
}

/** Director/Executive/Super Admin only (gated by whoever opens the modal — see IncidentDashboardPage).
 *  reportedById is filled in by the caller from the logged-in identity; the backend re-derives and
 *  overwrites it from the JWT regardless, same pattern as CreateTaskForm/createdById. */
export function CreateIncidentForm({ onSubmit, onCancel, submitting }: CreateIncidentFormProps) {
  const { currentUser } = useAuth()
  const peopleQuery = usePeople()
  const businessUnits = useBusinessUnitOptions()

  const [dateOccurred, setDateOccurred] = useState('')
  const [dateDiscovered, setDateDiscovered] = useState('')
  const [dateReported, setDateReported] = useState('')
  const [businessUnit, setBusinessUnit] = useState<IncidentBusinessUnit | ''>('')
  const [locationChannel, setLocationChannel] = useState('')
  const [category, setCategory] = useState<IncidentCategory | ''>('')
  const [eventType, setEventType] = useState('')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [likelihood, setLikelihood] = useState('')
  const [impact, setImpact] = useState('')
  const [customersAffected, setCustomersAffected] = useState('')
  const [serviceDowntimeMinutes, setServiceDowntimeMinutes] = useState('')
  const [grossLoss, setGrossLoss] = useState('')
  const [recovery, setRecovery] = useState('')
  const [dataBreach, setDataBreach] = useState<DataBreachStatus>('NO')
  const [regulatorNotifiable, setRegulatorNotifiable] = useState<RegulatorNotifiableStatus>('NO')
  const [immediateContainment, setImmediateContainment] = useState('')
  const [actionOwnerId, setActionOwnerId] = useState('')
  const [targetClosureDate, setTargetClosureDate] = useState('')
  const [incidentOwner, setIncidentOwner] = useState('')

  const inherentScore = useMemo(
    () => computeInherentScore(likelihood ? Number(likelihood) : null, impact ? Number(impact) : null),
    [likelihood, impact],
  )
  const severity = useMemo(() => computeSeverity(inherentScore), [inherentScore])
  const netLoss = useMemo(
    () => computeNetLoss(grossLoss ? Number(grossLoss) : 0, recovery ? Number(recovery) : 0),
    [grossLoss, recovery],
  )

  const isValid =
    dateOccurred !== '' && dateDiscovered !== '' && dateReported !== '' &&
    businessUnit !== '' && category !== '' && title.trim() !== ''

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || businessUnit === '' || category === '') return

    onSubmit({
      dateOccurred,
      dateDiscovered,
      dateReported,
      businessUnit,
      locationChannel: locationChannel.trim() || undefined,
      category,
      eventType: eventType.trim() || undefined,
      title: title.trim(),
      description: description.trim() || undefined,
      likelihood: likelihood ? Number(likelihood) : undefined,
      impact: impact ? Number(impact) : undefined,
      customersAffected: customersAffected ? Number(customersAffected) : undefined,
      serviceDowntimeMinutes: serviceDowntimeMinutes ? Number(serviceDowntimeMinutes) : undefined,
      grossLoss: grossLoss ? Number(grossLoss) : undefined,
      recovery: recovery ? Number(recovery) : undefined,
      dataBreach,
      regulatorNotifiable,
      immediateContainment: immediateContainment.trim() || undefined,
      actionOwnerId: actionOwnerId ? Number(actionOwnerId) : undefined,
      targetClosureDate: targetClosureDate || undefined,
      // Overwritten by the backend from the JWT regardless — see IncidentController.createIncident.
      reportedById: currentUser?.id ?? 0,
      incidentOwner: incidentOwner.trim() || undefined,
    })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <TextField label="Title" value={title} onChange={setTitle} placeholder="What happened, in a few words" required />
      <TextField label="Description" value={description} onChange={setDescription} placeholder="Optional detail" />

      <div className={styles.row}>
        <TextField label="Date occurred" type="date" value={dateOccurred} onChange={setDateOccurred} required />
        <TextField label="Date discovered" type="date" value={dateDiscovered} onChange={setDateDiscovered} required />
        <TextField label="Date reported" type="date" value={dateReported} onChange={setDateReported} required />
      </div>

      <div className={styles.row}>
        <SelectField
          label="Business unit / branch"
          value={businessUnit}
          onChange={(v) => setBusinessUnit(v as IncidentBusinessUnit)}
          placeholder={businessUnits.isLoading ? 'Loading…' : 'Select a unit'}
          options={businessUnits.options}
        />
        <SelectField
          label="Incident category"
          value={category}
          onChange={(v) => setCategory(v as IncidentCategory)}
          placeholder="Select a category"
          options={CATEGORIES.map((c) => ({ label: categoryLabel(c), value: c }))}
        />
      </div>

      <div className={styles.row}>
        <TextField label="Location / channel" value={locationChannel} onChange={setLocationChannel} placeholder="e.g. Head Office / Core Banking" />
        <TextField label="Event type" value={eventType} onChange={setEventType} placeholder="e.g. System outage" />
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
        <SelectField
          label="Data / privacy breach?"
          value={dataBreach}
          onChange={(v) => setDataBreach(v as DataBreachStatus)}
          options={DATA_BREACH_OPTIONS}
        />
        <SelectField
          label="Regulator notifiable?"
          value={regulatorNotifiable}
          onChange={(v) => setRegulatorNotifiable(v as RegulatorNotifiableStatus)}
          options={REGULATOR_OPTIONS}
        />
      </div>

      <TextField label="Immediate containment" value={immediateContainment} onChange={setImmediateContainment} placeholder="What was done right away" />

      <div className={styles.row}>
        <SelectField
          label="Action owner (optional)"
          value={actionOwnerId}
          onChange={setActionOwnerId}
          placeholder={peopleQuery.isLoading ? 'Loading…' : 'Not yet assigned'}
          options={(peopleQuery.data ?? []).map((p) => ({ label: p.fullName, value: String(p.id) }))}
        />
        <TextField label="Target closure date" type="date" value={targetClosureDate} onChange={setTargetClosureDate} min={todayDate()} />
      </div>

      <TextField
        label="Incident owner (optional)"
        value={incidentOwner}
        onChange={setIncidentOwner}
        placeholder="Free text — e.g. who in the department this is associated with"
      />

      <div className={styles.actions}>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" variant="primary" disabled={!isValid || submitting}>
          {submitting ? 'Reporting…' : 'Report incident'}
        </Button>
      </div>
    </form>
  )
}
