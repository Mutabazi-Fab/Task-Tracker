import type { TaskStatus } from '../types/task.types'

/**
 * Mirrors the backend's derivation exactly, for client-side preview only
 * (e.g. showing a live status chip while typing a percentage before submit).
 * The status shown anywhere after a server response always comes from the
 * response itself — this is never used to set status on a request.
 */
export function deriveStatus(percentage: number): TaskStatus {
  if (percentage <= 0) return 'PENDING'
  if (percentage >= 100) return 'COMPLETED'
  return 'ONGOING'
}
