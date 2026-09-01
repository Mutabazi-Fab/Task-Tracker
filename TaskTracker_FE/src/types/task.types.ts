import type { TaskComment } from './comment.types'
import type { TaskReassignment } from './reassignment.types'

export type AssigneeType = 'INDIVIDUAL' | 'TEAM'

/** Always derived from progressPercentage server-side — never a form field. */
export type TaskStatus = 'PENDING' | 'ONGOING' | 'COMPLETED'

/** Who structured a subtask — the Director themself, or the Team Leader of the team
 *  owning its parent task. Null for a task that predates the hierarchy. */
export type CreatedByRole = 'DIRECTOR' | 'TEAM_LEADER'

/** One subtask under a top-level task, as shown on the parent's detail view. */
export interface SubtaskSummary {
  id: number
  taskCode: string
  title: string
  assigneeName: string
  status: TaskStatus
  progressPercentage: number
  createdByRole: CreatedByRole
}

/** One point on a task's (or the org's) progress-over-time trend. */
export interface TaskTimelinePoint {
  percentage: number
  date: string
  commentId: number
}

/** Row shape for the task list — carries the latest comment only. */
export interface TaskListItem {
  id: number
  taskCode: string
  title: string
  assigneeName: string
  assigneeType: AssigneeType
  status: TaskStatus
  progressPercentage: number
  dateAssigned: string
  assignedByName: string
  reassignmentCount: number
  lastComment: TaskComment | null
}

/** Full detail — complete comment + reassignment history, oldest first. */
export interface TaskDetail {
  id: number
  taskCode: string
  title: string
  description: string | null
  assigneeName: string
  assigneeId: number | null
  assigneeType: AssigneeType
  // The team actually responsible for this task regardless of assigneeType: for a
  // top-level task, same as assigneeId; for a subtask, its parent task's team. Used to
  // decide whether the current viewer is this task's Team Leader (who, along with a
  // Director/Super Admin, may reassign it) — see TaskDetailPage.
  owningTeamId: number | null
  status: TaskStatus
  progressPercentage: number
  dateAssigned: string
  assignedByName: string
  assignedById: number
  // null = top-level task (always team-assigned). Non-null = a subtask (always
  // individual-assigned, can't have subtasks of its own) — see ReassignTaskModal,
  // which uses this to decide "reassign to a team" vs "reassign to a person".
  parentTaskId: number | null
  parentTaskCode: string | null
  createdByRole: CreatedByRole | null
  subtasks: SubtaskSummary[]
  comments: TaskComment[]
  reassignments: TaskReassignment[]
  progressTimeline: TaskTimelinePoint[]
  createdAt: string
  updatedAt: string
}

/** Body for POST /tasks — a TOP-LEVEL task only. Director/Super-Admin-only, always
 *  team-assigned (no assigneeType choice — that's structural now, not a free pick). Must
 *  always carry the opening comment that explains 0%. */
export interface CreateTaskRequest {
  title: string
  description?: string
  createdById: number
  assignedTeamId: number
  dateAssigned: string
  openingNote: string
}

/** Body for POST /tasks/{parentTaskId}/subtasks. createdById must be either the parent
 *  task's Team Leader or a Director/Super Admin (the Director/Super Admin bypassing the
 *  Team Leader is explicitly allowed); assignedPersonId must be a member of the parent
 *  task's team. */
export interface CreateSubtaskRequest {
  title: string
  description?: string
  createdById: number
  assignedPersonId: number
  dateAssigned: string
  openingNote: string
}

/** Body for PUT /tasks/{id}. Title/description/dateAssigned only — never progress or assignee. */
export interface UpdateTaskRequest {
  title: string
  description?: string
  dateAssigned: string
}

/** Spring Data Page<T> envelope, as returned by GET /tasks. */
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
