/** Global role, ascending: MEMBER < DIRECTOR < EXECUTIVE < SUPER_ADMIN. "Team Leader" is
 *  scoped per-team instead (see TeamMember on the backend), not a value here. Null for a
 *  person created before roles existed and never migrated. Executive is the CEO's seat —
 *  everything a Director can do, plus org-wide task creation at the Department level and
 *  the executive dashboard (see useAuth's isDirector/isExecutive/isSuperAdmin). Super Admin
 *  has every Executive permission plus a few exclusively its own (role changes, account
 *  activation, department administration). */
export type Role = 'DIRECTOR' | 'EXECUTIVE' | 'MEMBER' | 'SUPER_ADMIN'

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
  /** Every person belongs to exactly one Department, independent of team membership —
   *  null only for an account that predates this field. */
  departmentName: string | null
  departmentId: number | null
}

/**
 * Body for POST /people (and reused for PUT /people/{id}, which ignores createdById/role/
 * departmentId/password). There is no public self-registration — createdById must resolve
 * to a Super Admin, enforced server-side; departmentId and password (at least 8 characters)
 * are both required at creation. The password set here is what the new person logs in with
 * — the Super Admin is expected to hand it to them directly.
 */
export interface CreatePersonRequest {
  fullName: string
  email: string
  jobTitle: string
  rank?: string
  createdById?: number
  role?: Role
  departmentId?: number
  password?: string
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

/** Body for POST /people/{id}/send-password-reset. Super-Admin-only; reason is mandatory
 *  (both here and server-side). Fails server-side if this person has never signed up. */
export interface SendPasswordResetRequest {
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

/** One row of GET /people/account-status-changes — the org-wide (de)activation audit log.
 *  active is the status this change moved the account TO. */
export interface AccountStatusChangeActivity {
  id: number
  personId: number
  personName: string
  active: boolean
  changedByName: string
  reason: string
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
