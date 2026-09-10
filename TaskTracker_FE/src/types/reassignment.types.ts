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
 * structural, not a free choice: a TEAM/INDIVIDUAL-assigned task moves via newTeamId/
 * newPersonId (scoped to the parent's team for an ordinary leaf subtask, org-wide
 * otherwise — see ReassignTaskModal); a DEPARTMENT-assigned task moves to a different
 * Department entirely via newDepartmentId (Executive/Super Admin only). Reason is
 * mandatory server-side.
 */
export interface ReassignTaskRequest {
  newTeamId?: number
  newPersonId?: number
  newDepartmentId?: number
  reassignedById: number
  reason: string
}
