/** Verbatim from the source Excel's "Lists & Guidance" sheet. This is the same text the
 *  app's own severity bands (IncidentMapper.computeSeverity) and closure enforcement
 *  (IncidentServiceImpl.requireClosureReadiness) are built from — it's shown here as
 *  read-only reference, not editable, so it can never drift from what the system actually
 *  does. For anything org-specific beyond this fixed sheet (escalation contacts changing,
 *  onboarding notes for a new Director/CEO), see the editable Guidance Notes section below
 *  it on the page instead. */

export interface SeverityRow {
  severity: string
  scoreRange: string
  criteria: string
  escalation: string
  targetInitialReport: string
}

export const SEVERITY_TABLE: SeverityRow[] = [
  {
    severity: 'Critical',
    scoreRange: '16 - 25',
    criteria:
      'Severe financial/customer impact; prolonged critical-service outage; confirmed material cyber/data breach; major fraud; BCM or crisis activation.',
    escalation: 'CEO, CRO/Head of Risk, relevant executives, BRC Chair as applicable; assess BNR/authority notification.',
    targetInitialReport: 'Immediately / within 1 hour',
  },
  {
    severity: 'High',
    scoreRange: '11 - 15',
    criteria: 'Material disruption or loss; significant control failure; multiple customers; likely regulatory or reputational concern.',
    escalation: 'Relevant Executive and Risk/Compliance/Information Security, as applicable.',
    targetInitialReport: 'Within 4 hours',
  },
  {
    severity: 'Moderate',
    scoreRange: '6 - 10',
    criteria: 'Contained impact; limited customers or operational disruption; manageable through normal management action.',
    escalation: 'Department Head and Risk Management.',
    targetInitialReport: 'Within 24 hours',
  },
  {
    severity: 'Low',
    scoreRange: '1 - 5',
    criteria: 'Minor event or near miss with negligible impact and prompt correction.',
    escalation: 'Line manager; include in routine incident reporting.',
    targetInitialReport: 'Within 2 working days',
  },
]

export interface ScoreRow {
  score: number
  impactMeaning: string
  likelihoodMeaning: string
}

export const SCORE_SCALE: ScoreRow[] = [
  { score: 1, impactMeaning: 'Insignificant', likelihoodMeaning: 'Rare' },
  { score: 2, impactMeaning: 'Minor', likelihoodMeaning: 'Unlikely' },
  { score: 3, impactMeaning: 'Moderate', likelihoodMeaning: 'Possible' },
  { score: 4, impactMeaning: 'Major', likelihoodMeaning: 'Likely' },
  { score: 5, impactMeaning: 'Severe', likelihoodMeaning: 'Almost certain' },
]

export interface CompletionRequirement {
  title: string
  description: string
}

export const MINIMUM_COMPLETION_REQUIREMENTS: CompletionRequirement[] = [
  { title: '1. Record promptly', description: 'Log the incident as soon as discovered; do not wait for the investigation to finish.' },
  {
    title: '2. Preserve evidence',
    description: 'Keep incident tickets, transaction references, reconciliations, system logs and approvals in the approved repository.',
  },
  {
    title: '3. Protect information',
    description: 'Use customer or staff identifiers only where necessary; never record passwords, PINs or full payment-card details.',
  },
  {
    title: '4. Quantify impact',
    description: 'Record gross loss, recoveries, net loss, downtime and customers affected; update amounts as evidence is validated.',
  },
  {
    title: '5. Analyse cause',
    description: 'Identify the underlying people, process, system, external-event or governance cause — not only the immediate symptom.',
  },
  {
    title: '6. Track actions',
    description: 'Assign one accountable owner and target date for each corrective/preventive action; overdue items require escalation.',
  },
  {
    title: '7. Close formally',
    description: 'Close only when actions and evidence are complete and required Risk, Compliance, IT Security or Legal reviews are recorded.',
  },
]

/** From the source Excel's Dashboard sheet — the "Management attention rules" text block. */
export const MANAGEMENT_ATTENTION_RULES =
  'Immediate escalation is required for: Critical incidents; suspected fraud; material customer, financial, regulatory or reputational ' +
  'impact; personal-data compromise; significant core-banking/cyber outage; BCM activation; or any incident that may require ' +
  'notification to BNR or another authority. Incident closure requires evidence of root-cause analysis, completed corrective actions, ' +
  'loss validation and Risk/Compliance review where applicable.'
