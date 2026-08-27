/** One entry in a task's ownership audit trail. Immutable. */
export interface TaskReassignment {
  id: number
  fromName: string
  toName: string
  reassignedByName: string
  reason: string
  reassignedAt: string
}

/**
 * Body for POST /tasks/{id}/reassign. No newAssigneeType — which field applies is
 * structural, not a free choice: a top-level task can only move to a different TEAM
 * (newTeamId), a subtask can only move to a different PERSON who's a member of the team
 * that owns its parent task (newPersonId). Reason is mandatory server-side.
 */
export interface ReassignTaskRequest {
  newTeamId?: number
  newPersonId?: number
  reassignedById: number
  reason: string
}
