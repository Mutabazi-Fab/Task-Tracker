/** Extensible on the backend — new types can appear without a frontend release breaking,
 *  since the UI just renders `message` regardless of which of these it is. */
export type NotificationType = 'TEAM_MEMBER_ADDED' | 'TEAM_MEMBER_REMOVED' | 'ROLE_CHANGED' | 'ACCOUNT_STATUS_CHANGED'

export interface Notification {
  id: number
  type: NotificationType
  message: string
  relatedEntityId: number | null
  isRead: boolean
  createdAt: string
}
