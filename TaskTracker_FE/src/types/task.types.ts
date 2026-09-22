import type { TaskComment } from './comment.types'
import type { TaskReassignment } from './reassignment.types'
import type { DeadlineExtension } from './deadlineExtension.types'
import type { TaskDocument } from './document.types'
import type { Role } from './person.types'

/** DEPARTMENT is Executive-only and only ever at depth 0 — its head Director then turns it into a real TEAM-/INDIVIDUAL-assigned "implementation task" one level deeper. */
export type AssigneeType = 'INDIVIDUAL' | 'TEAM' | 'DEPARTMENT'

/** Always derived from progressPercentage server-side — never a form field. */
export type TaskStatus = 'PENDING' | 'ONGOING' | 'COMPLETED'

/** 'updatedAt,desc' (default) surfaces actively-worked tasks, not just newly created ones; 'createdAt,desc' is "what did I just set up"; 'none' sends no sort param at all. */
export type TaskSortValue = 'updatedAt,desc' | 'createdAt,desc' | 'none'

/** Who originated a task — open text matched against TaskSourceCategory's saved list, not a fixed set of literals. Paired with a free-text sourceLabel (e.g. "GPO", "E&Y"). */
export type TaskSource = string

/** Executive/Super-Admin-only, settable at creation only. CRITICAL sets pinned = true as a one-time default at creation — pinning stays independently editable afterward. */
export type TaskSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

/** One entry in the Source dropdown itself (e.g. "Initiative", "Regulator") — a saved, open list. Adding a new one is Executive-or-above, enforced server-side. */
export interface TaskSourceCategory {
  id: number
  name: string
}

/** A saved, reusable "Source Detail" suggestion for one Source category (e.g. "BNR" under Regulator) — never a restriction on what can be typed. */
export interface TaskSourceEntry {
  id: number
  source: TaskSource
  label: string
}

/** Who structured a subtask — the Director, or the Team Leader of the team owning its parent task. Null for a task predating the hierarchy. */
export type CreatedByRole = 'DIRECTOR' | 'TEAM_LEADER'

/** One subtask under a top-level task. assigneeType tells a TEAM-assigned "implementation task" apart from an ordinary INDIVIDUAL leaf subtask. */
export interface SubtaskSummary {
  id: number
  taskCode: string
  title: string
  assigneeName: string
  // Null unless assigneeType is INDIVIDUAL.
  assigneeId: number | null
  assigneeType: AssigneeType
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
  deadline: string | null // null only for a task that predates this field
  source: TaskSource | null
  sourceLabel: string | null // both null unless the creator recorded where it originated
  severity: TaskSeverity | null // null unless an Executive/Super Admin set it at creation
  pinned: boolean // manual, independently-editable toggle — see SetPinnedRequest
  assignedByName: string
  reassignmentCount: number
  lastComment: TaskComment | null
  // Distinguishes "assigned to one person directly" from "a subtask of something" — both
  // share assigneeType 'INDIVIDUAL' with nothing else to tell them apart.
  parentTaskCode: string | null
  parentTaskTitle: string | null // for "under {title}" UI copy; null wherever parentTaskCode is
  depth: number // 0 top-level, 1 direct child, 2 grandchild (Department-rooted only)
  // Empty for a leaf subtask — see DailyGoalCard, which flattens this to offer a member's
  // own subtasks as daily-goal candidates alongside their directly-assigned top-level tasks.
  subtasks: SubtaskSummary[]
  createdAt: string // backs the "New" badge (see isRecentlyCreated), compared at render time
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
  // The department this task lives in, regardless of assigneeType — gates pin/reassign UI
  // (a plain Director may only act within their own department) against currentUser.departmentId.
  taskDepartmentId: number | null
  // The team actually responsible for this task: same as assigneeId for a top-level task,
  // else its parent task's team. Used to decide if the viewer is this task's Team Leader.
  owningTeamId: number | null
  status: TaskStatus
  progressPercentage: number
  dateAssigned: string
  deadline: string | null // null only for a task that predates this field
  source: TaskSource | null
  sourceLabel: string | null
  severity: TaskSeverity | null // null unless an Executive/Super Admin set it at creation
  pinned: boolean // manual toggle, not derived from severity — see SetPinnedRequest
  assignedByName: string
  assignedById: number
  assignedByRole: Role | null // shown next to their name, e.g. "· Director" vs "· Executive"
  // Who actually decides a deadline extension — a Director-or-above, always, even when
  // assignedById is a mere Team Leader. Gate deadline-decision UI off THIS field, not assignedById.
  deadlineDeciderName: string
  deadlineDeciderId: number
  // null = top-level task. Non-null = a subtask — see ReassignTaskModal, which uses this
  // to decide "reassign to a team" vs "reassign to a person".
  parentTaskId: number | null
  parentTaskCode: string | null
  parentTaskTitle: string | null // for a "back to {title}" breadcrumb via parentTaskId
  createdByRole: CreatedByRole | null
  depth: number // 0 top-level, 1 direct child, 2 grandchild (Department-rooted only)
  subtasks: SubtaskSummary[]
  comments: TaskComment[]
  reassignments: TaskReassignment[]
  deadlineExtensions: DeadlineExtension[]
  documents: TaskDocument[]
  progressTimeline: TaskTimelinePoint[]
  createdAt: string
  updatedAt: string
}

/** Body for POST /tasks — a TOP-LEVEL (depth 0) task. Director-or-above. Exactly one of assignedTeamId/assignedPersonId/assignedDepartmentId must be set (Department is Executive-only). */
export interface CreateTaskRequest {
  title: string
  description?: string
  createdById: number
  assignedTeamId?: number
  assignedPersonId?: number
  assignedDepartmentId?: number
  dateAssigned: string
  deadline: string
  source?: TaskSource
  sourceLabel?: string
  severity?: TaskSeverity // Executive/Super-Admin-only — rejected server-side otherwise
  openingNote: string
}

/** Body for POST /tasks/{parentTaskId}/subtasks. Two shapes depending on the parent's
 *  assigneeType: TEAM-assigned parent → the classic leaf-subtask case (assignedPersonId
 *  required, must be a team member); DEPARTMENT-assigned parent → the "implementation
 *  task" case (exactly one of assignedTeamId/assignedPersonId, org-wide, no team-membership
 *  restriction). */
export interface CreateSubtaskRequest {
  title: string
  description?: string
  createdById: number
  assignedPersonId?: number
  assignedTeamId?: number
  dateAssigned: string
  deadline: string
  source?: TaskSource
  sourceLabel?: string
  severity?: TaskSeverity
  openingNote: string
}

/** Body for PUT /tasks/{id}/pin. Director-or-above only. A manual, independently-editable
 *  toggle — not derived from severity. */
export interface SetPinnedRequest {
  pinned: boolean
  changedById: number
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
 *  Director or Super Admin only. Everything here is a snapshot at the moment of the event,
 *  not a live lookup — a DELETED row's task no longer exists to look up. */
export interface TaskActivity {
  id: number
  action: TaskActivityAction
  taskCode: string
  title: string
  parentTaskCode: string | null // set only when the task was a subtask
  parentTaskTitle: string | null // snapshot, same reasoning as parentTaskCode
  assigneeType: AssigneeType
  assigneeSummary: string
  performedByName: string
  timestamp: string
}
