/** Set once at creation (PENDING), then once more on decision. A direct extension (see
 *  ExtendDeadlineRequest) is logged as APPROVED from the start, self-decided. */
export type ExtensionRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/** One row of a task's deadline-extension history. decidedByName/decisionNote/decidedAt
 *  are all null while status is PENDING. */
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
 *  accountable person or a Director-or-above override — enforced server-side.
 *  requestedDeadline must be after the current deadline. */
export interface RequestDeadlineExtensionRequest {
  requestedDeadline: string
  justification: string
  requestedById: number
}

/** Body for PUT /tasks/{id}/deadline-extensions/{extensionId}. decidedById must be the
 *  task's own setter or a Director-or-above override — enforced server-side. */
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

/** One row of the cross-task "Requests" inbox — every deadline-extension request still awaiting a
 *  decision from the viewer, org-wide. */
export interface PendingExtensionRequest {
  id: number
  taskId: number
  taskCode: string
  taskTitle: string
  currentDeadline: string | null
  requestedDeadline: string
  justification: string
  requestedByName: string
  requestedAt: string
  /** Viewer-specific — everyone who sees a row can reject it, but on a CEO-mandated chain
   *  only an Executive/Super Admin can approve. false means this viewer can reject but not
   *  approve — see PendingExtensionRequestItem. */
  canApprove: boolean
  /** Objective fact about the request. false on a CEO-mandated chain means it hasn't
   *  reached the CEO/Super Admin's inbox yet — see the "Send to CEO for approval" action. */
  forwardedToApprover: boolean
}
