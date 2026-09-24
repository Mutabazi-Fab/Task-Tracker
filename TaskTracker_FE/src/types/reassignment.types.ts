/** One entry in a task's ownership audit trail. Immutable. */
export interface TaskReassignment {
  id: number
  fromName: string
  toName: string
  reassignedByName: string
  reason: string
  reassignedAt: string
}

/** Body for POST /tasks/{id}/reassign. */
export interface ReassignTaskRequest {
  newTeamId?: number
  newPersonId?: number
  newDepartmentId?: number
  reassignedById: number
  reason: string
}
