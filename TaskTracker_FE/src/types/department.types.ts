/** The org-chart layer above Team — every Team and every Person belongs to exactly one
 *  Department. Administration (create/rename/change head) is Super-Admin-only; read
 *  access is open to any authenticated caller (a Director/Executive needs the list to
 *  pick one when creating a team, person, or department-level task). */
export interface Department {
  id: number
  name: string
  headDirectorName: string | null
  headDirectorId: number | null
  teamCount: number
  createdByName: string | null
  createdAt: string
}

/** Body for POST /departments. Super-Admin-only; headDirectorId must already hold the
 *  Director role or above. */
export interface CreateDepartmentRequest {
  name: string
  headDirectorId: number
  createdById: number
}

/** Body for PUT /departments/{id}. Rename only — head reassignment goes through its own
 *  endpoint below. Super-Admin-only. */
export interface RenameDepartmentRequest {
  name: string
  changedById: number
}

/** Body for PUT /departments/{id}/head. Super-Admin-only; newHeadDirectorId must already
 *  hold the Director role or above. */
export interface ChangeDepartmentHeadRequest {
  newHeadDirectorId: number
  changedById: number
}
