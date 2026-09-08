import type { TaskComment } from './comment.types'
import type { TaskReassignment } from './reassignment.types'

export type AssigneeType = 'INDIVIDUAL' | 'TEAM'

/** Always derived from progressPercentage server-side — never a form field. */
export type TaskStatus = 'PENDING' | 'ONGOING' | 'COMPLETED'

/** How task lists order themselves — 'updatedAt,desc'/'createdAt,desc' are passed straight
 *  through as Spring's `sort` query param (`property,direction`), same convention already
 *  used for the audit-log fetches; 'none' means exactly that — no sort param is sent at
 *  all, so the list comes back in whatever order the database naturally returns it, the
 *  same as before sorting existed.
 *  'updatedAt,desc' is the default everywhere: updatedAt is bumped by Hibernate on every
 *  save (a progress comment, a reassignment, a subtask's rollup touching its parent), so a
 *  brand-new task — whose updatedAt equals its createdAt at the moment it's made — already
 *  sorts to the top, and a task that's actively being worked stays visible even once it's
 *  no longer the newest thing created. 'createdAt,desc' is the explicit alternative for
 *  "what did I just set up", separate from "what's actually moving". */
export type TaskSortValue = 'updatedAt,desc' | 'createdAt,desc' | 'none'

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
  // Null for a top-level task (team- or individually-assigned) — set only for a real
  // subtask. Both a subtask and a standalone individual task share assigneeType
  // 'INDIVIDUAL' with nothing else to tell them apart, so this is what actually
  // distinguishes "assigned to one person directly" from "a subtask of something".
  parentTaskCode: string | null
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

/** Body for POST /tasks — a TOP-LEVEL task only. Director/Super-Admin-only. Assigned to
 *  either a team OR a single individual directly — exactly one of assignedTeamId/
 *  assignedPersonId must be set (the backend rejects both or neither). Must always carry
 *  the opening comment that explains 0%. */
export interface CreateTaskRequest {
  title: string
  description?: string
  createdById: number
  assignedTeamId?: number
  assignedPersonId?: number
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

export type TaskActivityAction = 'CREATED' | 'DELETED'

/** One row of GET /tasks/activity — every task/subtask created or deleted, org-wide.
 *  Director or Super Admin only. Everything here is a snapshot taken at the moment of the
 *  event, not a live lookup — a DELETED row's task no longer exists to look up, and a
 *  CREATED row should keep showing what the task looked like when it was made either way. */
export interface TaskActivity {
  id: number
  action: TaskActivityAction
  taskCode: string
  title: string
  /** Set only when the task was a subtask. */
  parentTaskCode: string | null
  assigneeType: AssigneeType
  assigneeSummary: string
  performedByName: string
  timestamp: string
}
