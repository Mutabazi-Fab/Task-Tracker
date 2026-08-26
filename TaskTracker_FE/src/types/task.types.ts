import type { TaskComment } from './comment.types'
import type { TaskReassignment } from './reassignment.types'

export type AssigneeType = 'INDIVIDUAL' | 'TEAM'

/** Always derived from progressPercentage server-side — never a form field. */
export type TaskStatus = 'PENDING' | 'ONGOING' | 'COMPLETED'

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
  status: TaskStatus
  progressPercentage: number
  dateAssigned: string
  assignedByName: string
  assignedById: number
  comments: TaskComment[]
  reassignments: TaskReassignment[]
  progressTimeline: TaskTimelinePoint[]
  createdAt: string
  updatedAt: string
}

/** Body for POST /tasks. Must always carry the comment that explains 0%. */
export interface CreateTaskRequest {
  title: string
  description?: string
  assignedById: number
  assigneeType: AssigneeType
  assignedPersonId?: number
  assignedTeamId?: number
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
