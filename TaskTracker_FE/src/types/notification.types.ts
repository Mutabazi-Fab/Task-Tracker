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
  | 'DEADLINE_EXTENSION_REQUESTED'
  | 'DEADLINE_EXTENSION_APPROVED'
  | 'DEADLINE_EXTENSION_REJECTED'
  | 'DEADLINE_EXTENDED'
  | 'DISCUSSION_COMMENT_POSTED'
  | 'DISCUSSION_REPLY_POSTED'

export interface Notification {
  id: number
  type: NotificationType
  message: string
  relatedEntityId: number | null
  isRead: boolean
  createdAt: string
}
