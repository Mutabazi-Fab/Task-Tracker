/** Mirrors the backend's Incident module — see IT. Incident Register.xlsx and the backend's
 *  IncidentServiceImpl for where each field/rule comes from. */

/** A Department name (or "Other") — no longer a fixed list, so a newly created department is a valid
 *  choice straight away. Incidents recorded earlier are re-spelled readable by the backend. */
export type IncidentBusinessUnit = string

export type IncidentCategory =
  | 'INTERNAL_FRAUD' | 'EXTERNAL_FRAUD' | 'EMPLOYMENT_PRACTICES_SAFETY'
  | 'CLIENTS_PRODUCTS_BUSINESS_PRACTICES' | 'PHYSICAL_ASSET_DAMAGE' | 'ICT_SYSTEMS'
  | 'CYBERSECURITY' | 'PROCESS_EXECUTION' | 'BUSINESS_DISRUPTION_BCM' | 'COMPLIANCE_LEGAL'
  | 'THIRD_PARTY_OUTSOURCING'

/** Derived from Likelihood x Impact — never set directly. LOW/MODERATE/HIGH/CRITICAL,
 *  standardized on "Moderate" (the source Excel's own formula inconsistently said "Medium"). */
export type IncidentSeverity = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL'

/** Exactly the source Excel's Status dropdown — no extra states invented on top (Risk/
 *  Compliance review are their own fields, gating closure, not a status of their own). */
export type IncidentStatus = 'OPEN' | 'UNDER_INVESTIGATION' | 'MONITORING' | 'CLOSED' | 'REJECTED_NOT_AN_INCIDENT'

export type IncidentReviewStatus = 'PENDING' | 'COMPLETED' | 'NOT_REQUIRED'

export type DataBreachStatus = 'NO' | 'YES' | 'POTENTIAL'

export type RegulatorNotifiableStatus = 'NO' | 'YES' | 'ASSESS'

/** Computed fresh on every read (never persisted) — depends on TODAY(). CLOSED_LATE is new
 *  versus the source Excel, which never flagged a late close. */
export type ActionSlaStatus = 'ON_TRACK' | 'DUE_SOON' | 'OVERDUE' | 'NO_DUE_DATE' | 'CLOSED' | 'CLOSED_LATE'

export interface IncidentListItem {
  id: number
  incidentCode: string
  title: string
  businessUnit: IncidentBusinessUnit
  category: IncidentCategory
  severity: IncidentSeverity | null
  status: IncidentStatus
  dateOccurred: string
  targetClosureDate: string | null
  daysOpen: number
  actionSla: ActionSlaStatus
  netLoss: number
  actionOwnerName: string | null
  reportedByName: string
}

export interface IncidentStatusChange {
  id: number
  fromStatus: IncidentStatus | null
  toStatus: IncidentStatus
  changedByName: string
  note: string | null
  changedAt: string
}

export interface IncidentDetail {
  id: number
  incidentCode: string
  dateOccurred: string
  timeOccurred: string | null
  dateDiscovered: string
  dateReported: string
  businessUnit: IncidentBusinessUnit
  locationChannel: string | null
  category: IncidentCategory
  eventType: string | null
  title: string
  description: string | null
  likelihood: number | null
  impact: number | null
  inherentScore: number | null
  severity: IncidentSeverity | null
  customersAffected: number | null
  serviceDowntimeMinutes: number | null
  grossLoss: number
  recovery: number
  netLoss: number
  dataBreach: DataBreachStatus
  regulatorNotifiable: RegulatorNotifiableStatus
  bnrNotificationDate: string | null
  status: IncidentStatus
  immediateContainment: string | null
  rootCause: string | null
  correctiveAction: string | null
  actionOwnerId: number | null
  actionOwnerName: string | null
  targetClosureDate: string | null
  actualClosureDate: string | null
  daysOpen: number
  actionSla: ActionSlaStatus
  wasClosedLate: boolean
  evidenceReference: string | null
  reportedById: number
  reportedByName: string
  incidentOwner: string | null
  riskReview: IncidentReviewStatus
  complianceReview: IncidentReviewStatus
  lessonsLearned: string | null
  closureReady: boolean
  closureBlockers: string[]
  statusHistory: IncidentStatusChange[]
  createdAt: string
  updatedAt: string
}

/** Body for POST /incidents. Director/Executive/Super Admin only — enforced server-side.
 *  reportedById is overwritten server-side with the caller's real identity. */
export interface CreateIncidentRequest {
  dateOccurred: string
  timeOccurred?: string
  dateDiscovered: string
  dateReported: string
  businessUnit: IncidentBusinessUnit
  locationChannel?: string
  category: IncidentCategory
  eventType?: string
  title: string
  description?: string
  likelihood?: number
  impact?: number
  customersAffected?: number
  serviceDowntimeMinutes?: number
  grossLoss?: number
  recovery?: number
  dataBreach?: DataBreachStatus
  regulatorNotifiable?: RegulatorNotifiableStatus
  bnrNotificationDate?: string
  immediateContainment?: string
  rootCause?: string
  correctiveAction?: string
  actionOwnerId?: number
  targetClosureDate?: string
  evidenceReference?: string
  reportedById: number
  incidentOwner?: string
  lessonsLearned?: string
}

/** Body for PUT /incidents/{id}. Same shape as create, minus reportedById, plus
 *  actualClosureDate/riskReview/complianceReview (not settable at creation) and changedById. */
export interface UpdateIncidentRequest {
  dateOccurred?: string
  timeOccurred?: string
  dateDiscovered?: string
  dateReported?: string
  businessUnit?: IncidentBusinessUnit
  locationChannel?: string
  category?: IncidentCategory
  eventType?: string
  title: string
  description?: string
  likelihood?: number
  impact?: number
  customersAffected?: number
  serviceDowntimeMinutes?: number
  grossLoss?: number
  recovery?: number
  dataBreach?: DataBreachStatus
  regulatorNotifiable?: RegulatorNotifiableStatus
  bnrNotificationDate?: string
  immediateContainment?: string
  rootCause?: string
  correctiveAction?: string
  actionOwnerId?: number
  targetClosureDate?: string
  actualClosureDate?: string
  evidenceReference?: string
  incidentOwner?: string
  riskReview?: IncidentReviewStatus
  complianceReview?: IncidentReviewStatus
  lessonsLearned?: string
  changedById: number
}

/** Body for PUT /incidents/{id}/status. changedById is overwritten server-side. */
export interface ChangeIncidentStatusRequest {
  newStatus: IncidentStatus
  note?: string
  changedById: number
}

export interface IncidentCount {
  label: string
  count: number
}

export interface IncidentMonthlyCount {
  month: string
  count: number
}

export interface IncidentDashboard {
  totalIncidents: number
  openOrMonitoringCount: number
  criticalOrHighCount: number
  overdueActionsCount: number
  grossLossTotal: number
  recoveryTotal: number
  netLossTotal: number
  regulatorNotifiableCount: number
  statusBreakdown: IncidentCount[]
  severityBreakdown: IncidentCount[]
  categoryBreakdown: IncidentCount[]
  monthlyTrend: IncidentMonthlyCount[]
  closedCount: number
  closedLateCount: number
  slaComplianceRate: number
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
