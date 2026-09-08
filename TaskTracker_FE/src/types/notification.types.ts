/** Extensible on the backend — new types can appear without a frontend release breaking,
 *  since the UI just renders `message` regardless of which of these it is. */
export type NotificationType =
  | 'TEAM_MEMBER_ADDED'
  | 'TEAM_MEMBER_REMOVED'
  | 'TEAM_LEADER_CHANGED'
  | 'ROLE_CHANGED'
  | 'ACCOUNT_STATUS_CHANGED'
  | 'PASSWORD_RESET_REQUESTED'
  | 'TASK_STALLED'
  | 'TASK_ASSIGNED'
  | 'SUBTASK_ASSIGNED'
  | 'TASK_REASSIGNED'
  | 'SUBTASK_REASSIGNED'

export interface Notification {
  id: number
  type: NotificationType
  message: string
  relatedEntityId: number | null
  isRead: boolean
  createdAt: string
}
