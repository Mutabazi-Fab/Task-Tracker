import type { IncidentSeverity } from '../../../types/incident.types'

/** Mirrors the backend's IncidentMapper.computeInherentScore/computeSeverity exactly — this
 *  is a client-side PREVIEW only, live-updating as the form is filled in; the server always
 *  recomputes both from likelihood/impact independently and never trusts what's sent here. */
export function computeInherentScore(likelihood: number | null, impact: number | null): number | null {
  if (likelihood === null || impact === null) return null
  return likelihood * impact
}

export function computeSeverity(inherentScore: number | null): IncidentSeverity | null {
  if (inherentScore === null) return null
  if (inherentScore <= 5) return 'LOW'
  if (inherentScore <= 10) return 'MODERATE'
  if (inherentScore <= 15) return 'HIGH'
  return 'CRITICAL'
}

export function computeNetLoss(grossLoss: number, recovery: number): number {
  return Math.max(0, grossLoss - recovery)
}
