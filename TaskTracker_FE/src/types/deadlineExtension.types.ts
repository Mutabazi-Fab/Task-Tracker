/** The lifecycle of one deadline-extension request — set once at creation (PENDING), then
 *  exactly once more when a decision is made. A direct extension (see ExtendDeadlineRequest)
 *  is logged as APPROVED from the start, self-decided by whoever extended it. */
export type ExtensionRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/** One row of a task's deadline-extension history — a request, and however it was (or
 *  wasn't yet) decided. decidedByName/decisionNote/decidedAt are all null while status is
 *  PENDING. A direct extension shows up here exactly like a request that was immediately
 *  self-approved. */
export interface DeadlineExtension {
  id: number
  currentDeadline: string | null
  requestedDeadline: string
  justification: string
  requestedByName: string
  status: ExtensionRequestStatus
  decidedByName: string | null
  decisionNote: string | null
  requestedAt: string
  decidedAt: string | null
}

/** Body for POST /tasks/{id}/deadline-extensions. requestedById must be the task's own
 *  accountable person (its Team Leader, its individual assignee, or — for a Department
 *  task — that Department's head Director), or a Director-or-above override (Executive-
 *  or-above for a Department task) — enforced server-side. requestedDeadline must be
 *  after the task's current deadline. */
export interface RequestDeadlineExtensionRequest {
  requestedDeadline: string
  justification: string
  requestedById: number
}

/** Body for PUT /tasks/{id}/deadline-extensions/{extensionId}. decidedById must be the
 *  task's own setter (assignedBy) or a Director-or-above override (Executive-or-above for
 *  a Department task) — enforced server-side. decisionNote is optional. */
export interface DecideDeadlineExtensionRequest {
  approve: boolean
  decisionNote?: string
  decidedById: number
}

/** Body for PUT /tasks/{id}/deadline — a direct extension, no approval round-trip. Same
 *  authority as deciding a request. Still logged as a self-approved DeadlineExtension row. */
export interface ExtendDeadlineRequest {
  newDeadline: string
  reason?: string
  extendedById: number
}
