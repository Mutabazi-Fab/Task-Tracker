export interface Person {
  id: number
  fullName: string
  email: string
  role: string
  teamName: string | null
  teamId: number | null
}

/** Body for POST /people (and reused for PUT /people/{id}). */
export interface CreatePersonRequest {
  fullName: string
  email: string
  role: string
  teamId?: number
}

export interface PersonStatistics {
  averageProgress: number | null
  tasksAssigned: number
  tasksCompleted: number
  tasksOngoing: number
  tasksPending: number
  commentsLogged: number
  tasksHandedOff: number
  fullyCompleted: boolean
}

/** How this person relates to a task that shows up in their history. */
export type InvolvementLabel =
  | 'CURRENT_OWNER'
  | 'VIA_TEAM'
  | 'PREVIOUSLY_ASSIGNED'
  | 'COMMENTER_ONLY'
  | 'UNKNOWN'

export interface PersonTaskHistoryItem {
  taskId: number
  taskCode: string
  title: string
  involvementLabel: InvolvementLabel
}
