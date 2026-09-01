/** Global role, ascending: MEMBER < DIRECTOR < SUPER_ADMIN. "Team Leader" is scoped
 *  per-team instead (see TeamMember on the backend), not a value here. Null for a person
 *  created before roles existed and never migrated. Super Admin has every Director
 *  permission plus a few exclusively its own — see useAuth's isDirector/isSuperAdmin. */
export type Role = 'DIRECTOR' | 'MEMBER' | 'SUPER_ADMIN'

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
  emailVerified: boolean
  active: boolean
  teams: PersonTeamMembership[]
}

/**
 * Body for POST /people (and reused for PUT /people/{id}, which ignores createdById/role).
 * createdById must be a Director or Super Admin; only a Super Admin may set role to
 * anything other than Member (omitted/undefined defaults to Member).
 */
export interface CreatePersonRequest {
  fullName: string
  email: string
  jobTitle: string
  rank?: string
  createdById?: number
  role?: Role
}

/** Body for PUT /people/{id}/role. Super-Admin-only; reason is mandatory (both here and
 *  server-side) — no promotion or demotion goes on record without one. */
export interface ChangeRoleRequest {
  newRole: Role
  changedById: number
  reason: string
}

/** Body for PUT /people/{id}/active. Super-Admin-only; reason is mandatory (both here and
 *  server-side). */
export interface SetAccountActiveRequest {
  active: boolean
  changedById: number
  reason: string
}

/** One row of GET /people/role-changes — the org-wide role-change audit log. */
export interface RoleChangeActivity {
  id: number
  personId: number
  personName: string
  oldRole: Role | null
  newRole: Role
  changedByName: string
  reason: string | null
  timestamp: string
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
