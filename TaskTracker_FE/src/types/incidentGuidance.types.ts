export interface GuidanceNote {
  id: number
  title: string
  body: string
  createdByName: string
  createdAt: string
  updatedByName: string | null
  updatedAt: string
}

/** Body for POST /incidents/guidance-notes. Director/Executive/Super Admin only.
 *  createdById is overwritten server-side from the JWT. */
export interface CreateGuidanceNoteRequest {
  title: string
  body: string
  createdById: number
}

/** Body for PUT /incidents/guidance-notes/{id}. Same tier as creating one. */
export interface UpdateGuidanceNoteRequest {
  title: string
  body: string
  changedById: number
}
