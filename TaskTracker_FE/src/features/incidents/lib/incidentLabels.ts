import type {
  ActionSlaStatus,
  DataBreachStatus,
  IncidentBusinessUnit,
  IncidentCategory,
  IncidentReviewStatus,
  IncidentStatus,
  RegulatorNotifiableStatus,
} from '../../../types/incident.types'

/** Turns an ENUM_CONSTANT into "Enum constant" — good enough for every label below except
 *  the handful with real acronyms (ICT, BCM), which get an explicit override instead. */
function titleCase(value: string): string {
  const words = value.toLowerCase().split('_')
  return words.map((w, i) => (i === 0 ? w.charAt(0).toUpperCase() + w.slice(1) : w)).join(' ')
}

/** Already readable text — a department name, or an older incident's enum constant that the
 *  backend re-spells on the way out (IncidentMapper.displayBusinessUnit). */
export function businessUnitLabel(value: IncidentBusinessUnit): string {
  return value
}

const CATEGORY_LABELS: Record<IncidentCategory, string> = {
  INTERNAL_FRAUD: 'Internal Fraud',
  EXTERNAL_FRAUD: 'External Fraud',
  EMPLOYMENT_PRACTICES_SAFETY: 'Employment Practices / Safety',
  CLIENTS_PRODUCTS_BUSINESS_PRACTICES: 'Clients / Products / Business Practices',
  PHYSICAL_ASSET_DAMAGE: 'Physical Asset Damage',
  ICT_SYSTEMS: 'ICT / Systems',
  CYBERSECURITY: 'Cybersecurity',
  PROCESS_EXECUTION: 'Process Execution',
  BUSINESS_DISRUPTION_BCM: 'Business Disruption / BCM',
  COMPLIANCE_LEGAL: 'Compliance / Legal',
  THIRD_PARTY_OUTSOURCING: 'Third Party / Outsourcing',
}

export function categoryLabel(value: IncidentCategory): string {
  return CATEGORY_LABELS[value]
}

const STATUS_LABELS: Record<IncidentStatus, string> = {
  OPEN: 'Open',
  UNDER_INVESTIGATION: 'Under Investigation',
  MONITORING: 'Monitoring',
  CLOSED: 'Closed',
  REJECTED_NOT_AN_INCIDENT: 'Rejected / Not an Incident',
}

export function statusLabel(value: IncidentStatus): string {
  return STATUS_LABELS[value]
}

const ACTION_SLA_LABELS: Record<ActionSlaStatus, string> = {
  ON_TRACK: 'On track',
  DUE_SOON: 'Due ≤7 days',
  OVERDUE: 'Overdue',
  NO_DUE_DATE: 'No due date',
  CLOSED: 'Closed',
  CLOSED_LATE: 'Closed late',
}

export function actionSlaLabel(value: ActionSlaStatus): string {
  return ACTION_SLA_LABELS[value]
}

export function reviewStatusLabel(value: IncidentReviewStatus): string {
  return titleCase(value)
}

export function dataBreachLabel(value: DataBreachStatus): string {
  return titleCase(value)
}

export function regulatorNotifiableLabel(value: RegulatorNotifiableStatus): string {
  return titleCase(value)
}

export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-RW', { maximumFractionDigits: 0 }).format(amount) + ' RWF'
}
