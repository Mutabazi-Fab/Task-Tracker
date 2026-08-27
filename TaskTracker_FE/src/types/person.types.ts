/** Global role — DIRECTOR vs everyone else. "Team Leader" is scoped per-team
 *  instead (see TeamMember on the backend), not a value here. Null for a
 *  person created before roles existed and never migrated. */
export type Role = 'DIRECTOR' | 'MEMBER'

/** One team this person belongs to — a person can be on several at once. */
export interface PersonTeamMembership {
  teamId: number
  teamName: string
  isLeader: boolean
}

export interface Person {
  id: number
  fullName: string
  email: string
  jobTitle: string
  rank: string | null
  role: Role | null
  teams: PersonTeamMembership[]
}

/** Body for POST /people (and reused for PUT /people/{id}). Identity/profile only —
 *  team membership is managed through TeamService (POST/DELETE /teams/{id}/members),
 *  not here, since a person can belong to multiple teams. */
export interface CreatePersonRequest {
  fullName: string
  email: string
  jobTitle: string
  rank?: string
}

/** This person's stats scoped to one team they belong to — not one blended
 *  number across every team. */
export interface PersonTeamStatistics {
  teamId: number
  teamName: string
  averageProgress: number | null
  tasksAssigned: number
  tasksCompleted: number
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
  teamBreakdown: PersonTeamStatistics[]
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
