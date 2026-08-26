import type { AssigneeType } from './task.types'

/** One entry in a task's ownership audit trail. Immutable. */
export interface TaskReassignment {
  id: number
  fromName: string
  toName: string
  reassignedByName: string
  reason: string
  reassignedAt: string
}

/** Body for POST /tasks/{id}/reassign. Reason is mandatory server-side. */
export interface ReassignTaskRequest {
  newAssigneeType: AssigneeType
  newPersonId?: number
  newTeamId?: number
  reassignedById: number
  reason: string
}
