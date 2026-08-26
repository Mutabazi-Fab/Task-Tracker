export interface Team {
  id: number
  name: string
  leaderName: string | null
  leaderId: number | null
  memberCount: number
}

/** Body for POST /teams (and reused for PUT /teams/{id}). */
export interface CreateTeamRequest {
  name: string
  teamLeaderId?: number
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
