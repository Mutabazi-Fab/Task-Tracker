import type { TaskComment } from './comment.types'
import type { TaskReassignment } from './reassignment.types'
import type { DeadlineExtension } from './deadlineExtension.types'
import type { Role } from './person.types'

/** DEPARTMENT is Executive-only, and only ever at depth 0 — a whole Department's head
 *  Director then turns it into a real TEAM- or INDIVIDUAL-assigned "implementation task"
 *  (depth 1), exactly like a plain top-level task, one level deeper. */
export type AssigneeType = 'INDIVIDUAL' | 'TEAM' | 'DEPARTMENT'

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

/** Who actually originated a task — open to anyone creating it, at any depth. Paired with
 *  a free-text sourceLabel on the task itself (e.g. "Director Musoni", "GPO", "E&Y",
 *  "Board of Directors"). */
export type TaskSource = 'INITIATIVE' | 'AUDITOR' | 'REGULATOR' | 'BOARD'

/** Executive/Super-Admin-only, settable at creation only, at any depth. CRITICAL sets
 *  pinned = true as a one-time default at creation — pinning itself stays a separate,
 *  independently-editable toggle afterward (see SetPinnedRequest). */
export type TaskSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

/** Who structured a subtask — the Director themself, or the Team Leader of the team
 *  owning its parent task. Null for a task that predates the hierarchy. */
export type CreatedByRole = 'DIRECTOR' | 'TEAM_LEADER'

/** One subtask under a top-level task, as shown on the parent's detail view. assigneeType
 *  tells a depth-1 TEAM-assigned "implementation task" (which can itself be broken into
 *  further subtasks) apart from an ordinary INDIVIDUAL leaf subtask (which never can). */
export interface SubtaskSummary {
  id: number
  taskCode: string
  title: string
  assigneeName: string
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
  // Null only for a task that predates this field.
  deadline: string | null
  // Both null unless this task's creator recorded where it originated.
  source: TaskSource | null
  sourceLabel: string | null
  // Null unless an Executive/Super Admin set it at creation.
  severity: TaskSeverity | null
  // A manual, independently-editable toggle — see SetPinnedRequest.
  pinned: boolean
  assignedByName: string
  reassignmentCount: number
  lastComment: TaskComment | null
  // Null for a top-level task (team- or individually-assigned) — set only for a real
  // subtask. Both a subtask and a standalone individual task share assigneeType
  // 'INDIVIDUAL' with nothing else to tell them apart, so this is what actually
  // distinguishes "assigned to one person directly" from "a subtask of something".
  parentTaskCode: string | null
  // 0 for a real top-level task (plain or Department-assigned), 1 for a direct child, 2
  // for a grandchild (only possible under a Department-rooted hierarchy).
  depth: number
  // Backs the "New" badge (see isRecentlyCreated) — compared against "now" at render time
  // rather than a precomputed boolean, so the badge disappears on its own as time passes.
  createdAt: string
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
  // Null only for a task that predates this field. Extended directly by whoever set it
  // (assignedById), or via the request/approve workflow — see deadlineExtensions below.
  deadline: string | null
  // Both null unless this task's creator recorded where it originated.
  source: TaskSource | null
  sourceLabel: string | null
  // Null unless an Executive/Super Admin set it at creation.
  severity: TaskSeverity | null
  // A manual, independently-editable toggle — see SetPinnedRequest. Not derived from
  // severity; a CRITICAL task can be freely un-pinned once it's on track.
  pinned: boolean
  assignedByName: string
  assignedById: number
  // Whoever set this task's deadline/scope — shown next to their name (e.g. "· Director"
  // vs "· Executive") so it's clear at a glance which tier a task actually came from.
  assignedByRole: Role | null
  // Who actually decides a deadline extension on this task — a Director-or-above,
  // always, even when assignedById is a mere Team Leader (who can create a leaf subtask
  // but has no authority over its deadline). Usually the same as assignedById/
  // assignedByName above, but not always — gate deadline-decision UI off THIS field, not
  // assignedById (chain of command: whoever's doing the work requests, a real Director
  // decides).
  deadlineDeciderName: string
  deadlineDeciderId: number
  // null = top-level task (always team-assigned). Non-null = a subtask (always
  // individual-assigned, can't have subtasks of its own) — see ReassignTaskModal,
  // which uses this to decide "reassign to a team" vs "reassign to a person".
  parentTaskId: number | null
  parentTaskCode: string | null
  createdByRole: CreatedByRole | null
  // 0 for a real top-level task (plain or Department-assigned), 1 for a direct child, 2
  // for a grandchild (only possible under a Department-rooted hierarchy). Decides whether
  // "Add subtask" is even offered, and which form shape it uses.
  depth: number
  subtasks: SubtaskSummary[]
  comments: TaskComment[]
  reassignments: TaskReassignment[]
  deadlineExtensions: DeadlineExtension[]
  progressTimeline: TaskTimelinePoint[]
  createdAt: string
  updatedAt: string
}

/** Body for POST /tasks — a TOP-LEVEL (depth 0) task. Director-or-above. Assigned to
 *  exactly one of a team, a single individual, or (Executive/Super Admin only) a whole
 *  Department — exactly one of assignedTeamId/assignedPersonId/assignedDepartmentId must
 *  be set (the backend rejects zero or more than one). Must always carry the opening
 *  comment that explains 0%. */
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
  // Executive/Super-Admin-only — the backend rejects a non-Executive creator's attempt
  // to set this.
  severity?: TaskSeverity
  openingNote: string
}

/**
 * Body for POST /tasks/{parentTaskId}/subtasks. Two shapes, depending on the parent
 * task's own assigneeType:
 *
 * Parent is TEAM-assigned (an ordinary top-level task, or a depth-1 Department
 * implementation task): the classic leaf-subtask case. createdById must be either the
 * parent task's Team Leader or a Director/Super Admin; assignedPersonId is required and
 * must be a member of the parent task's team; assignedTeamId must be omitted.
 *
 * Parent is DEPARTMENT-assigned (a depth-0 Executive task): the "implementation task"
 * case. createdById must be that department's head Director, or an Executive/Super Admin;
 * exactly one of assignedTeamId/assignedPersonId must be set, org-wide (no team-membership
 * restriction).
 */
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
