import type { TaskListItem } from './task.types'

/** Global role, ascending: MEMBER < DIRECTOR < EXECUTIVE < SUPER_ADMIN. "Team Leader" is
 *  scoped per-team instead, not a value here. Null for a legacy account never migrated.
 *  Executive is the CEO's seat — a Director's permissions plus Department-level task
 *  creation and the executive dashboard. Super Admin adds role changes, account
 *  activation, department administration (see useAuth's isDirector/isExecutive/isSuperAdmin). */
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
  active: boolean
  /** Whether this account must go through TOTP 2FA to log in — accounts created before
   *  2FA rolled out default to false and are never retroactively forced into it. */
  totpRequired: boolean
  /** Whether TOTP enrollment is actually complete (a QR was scanned and confirmed), as
   *  opposed to just required-but-still-pending. Only meaningful when totpRequired is
   *  true — determines whether a "Reset TOTP" admin action makes sense to show at all. */
  totpEnabled: boolean
  /** Null unless this person has an open password-reset request awaiting a Super Admin —
   *  set from the "Forgot Password?" flow. Drives the pending-request banner in
   *  PersonAdminControls. */
  pendingPasswordResetRequestedAt: string | null
  teams: PersonTeamMembership[]
  /** Every person belongs to exactly one Department — null only for a legacy account. */
  departmentName: string | null
  departmentId: number | null
}

/** Body for POST /people (reused for PUT /people/{id}, which ignores createdById/role/
 *  departmentId/password). createdById must resolve to a Super Admin, enforced
 *  server-side; departmentId and password (8+ chars) are required at creation. */
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

/** Body for POST /people/{id}/set-password. Super-Admin-only; reason is mandatory (both
 *  here and server-side). Sets the password directly — no code, no email, entirely
 *  offline — and never touches TOTP enrollment. If this person has a pending
 *  password-reset request, it's auto-marked fulfilled. */
export interface SetPasswordRequest {
  changedById: number
  newPassword: string
  reason: string
}

/** Body for POST /people/{id}/password-reset-request/dismiss. Super-Admin-only. No
 *  mandatory reason — dismissing is the lower-consequence action, nothing on the account
 *  actually changes. */
export interface DismissPasswordResetRequestRequest {
  changedById: number
}

/** Body for POST /people/{id}/reset-totp. Super-Admin-only; reason is mandatory. Fails
 *  server-side if this person was never enrolled in TOTP in the first place. */
export interface ResetTotpRequest {
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
  // Up to 3 tasks this person has personally flagged as "what I'm focused on today" — see
  // DailyGoalCard. Empty, not null/undefined, when none are set.
  dailyGoalTasks: TaskListItem[]
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
