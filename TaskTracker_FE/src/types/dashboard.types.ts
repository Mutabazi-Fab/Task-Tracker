import type { Person, PersonTeamStatistics } from './person.types'
import type { TaskListItem, TaskStatus } from './task.types'

export interface DashboardOverview {
  orgAverageProgress: number | null
  totalTasks: number
  completedCount: number
  ongoingCount: number
  pendingCount: number
}

/** One slice of the status donut. */
export interface StatusMix {
  status: TaskStatus
  count: number
  percentageShare: number
}

/** One point on the org-wide progress trend line. */
export interface ProgressPoint {
  date: string
  averagePercentage: number | null
}

export interface TeamLeaderboardItem {
  name: string
  leaderName: string | null
  averageProgress: number | null
  taskCount: number
  completedCount: number
}

export interface PersonSummary {
  name: string
  jobTitle: string
  averageProgress: number | null
  assignedCount: number
  completedCount: number
}

/** One person match in a global search — profile plus a per-team stats breakdown, not one
 *  blended number across every team they belong to (see PersonResultsSection). */
export interface PersonSearchResult {
  person: Person
  teamBreakdown: PersonTeamStatistics[]
}

export interface GlobalSearchResult {
  people: PersonSearchResult[]
  tasks: TaskListItem[]
}
