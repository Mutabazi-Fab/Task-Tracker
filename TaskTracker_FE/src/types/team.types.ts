export interface Team {
  id: number
  name: string
  createdByName: string
  createdById: number
  leaderName: string | null
  leaderId: number | null
  memberCount: number
  createdAt: string
  /** Every team belongs to exactly one Department — no team exists outside the org chart. */
  departmentName: string | null
  departmentId: number | null
}

/** One row of GET /teams/{id}/members — isLeader/joinedAt that plain Person doesn't carry, since membership is a join entity (a person can lead some teams and not others). */
export interface TeamMember {
  personId: number
  fullName: string
  jobTitle: string
  rank: string | null
  isLeader: boolean
  joinedAt: string
}

/** Body for POST /teams — Director creates the team, roster, and Team Leader in one
 *  request (leaderId must be one of memberIds). departmentId is required. */
export interface CreateTeamRequest {
  name: string
  createdById: number
  leaderId: number
  memberIds: number[]
  departmentId: number
}

/** Body for PUT /teams/{id}. Rename only — membership and leadership go through their own
 *  dedicated endpoints below, not this one. */
export interface UpdateTeamRequest {
  name: string
}

/** Body for POST /teams/{id}/members. Mandatory reason — no add can be submitted without one. */
export interface AddTeamMemberRequest {
  personId: number
  changedById: number
  reason: string
}

/** Body for DELETE /teams/{id}/members/{personId}. Mandatory reason, same as adding. */
export interface RemoveTeamMemberRequest {
  changedById: number
  reason: string
}

/** Body for PUT /teams/{id}/leader/{personId}. Director-only, no reason required. */
export interface SetTeamLeaderRequest {
  changedById: number
}

export type TeamMembershipChangeAction = 'ADDED' | 'REMOVED' | 'LEADER_CHANGED'

/** One row of a team's (append-only, never-updated) membership audit log. */
export interface TeamMembershipChange {
  id: number
  teamId: number
  teamName: string
  personId: number
  personName: string
  action: TeamMembershipChangeAction
  changedByName: string
  reason: string
  timestamp: string
}

export interface TeamMemberProgress {
  name: string
  averageProgress: number | null
}

export interface TeamStatistics {
  averageProgress: number | null
  taskCount: number
  memberCount: number
  completedCount: number
  memberProgresses: TeamMemberProgress[]
}
