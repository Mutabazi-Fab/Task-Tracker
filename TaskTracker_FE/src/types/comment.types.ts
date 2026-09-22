/** PROGRESS is a real percentage reading, the only kind that counts toward the trend
 *  chart. DISCUSSION is a plain Q&A message, fully open, threading one level deep via
 *  parentCommentId. */
export type CommentType = 'PROGRESS' | 'DISCUSSION'

/** One entry in a task's comment log — either a PROGRESS reading or a DISCUSSION message.
 *  Immutable — no edit/delete shape exists. */
export interface TaskComment {
  id: number
  sequenceNumber: number
  authorName: string
  percentageAtComment: number
  body: string
  type: CommentType
  /** Null for a PROGRESS comment, or a top-level DISCUSSION one. Set only for a DISCUSSION
   *  reply — the id of the top-level comment it replies to. */
  parentCommentId: number | null
  createdAt: string
}

/** Body for POST /tasks/{id}/comments — the progress log. percentageAtComment is optional
 *  server-side (a narrative-only entry), but the "Log progress" form always sends one. */
export interface AddCommentRequest {
  authorId: number
  percentageAtComment?: number
  body: string
}

/** Body for POST /tasks/{id}/discussion-comments — fully open, never touches percentage/
 *  status. parentCommentId is omitted for a top-level comment, or the id being replied to. */
export interface AddDiscussionCommentRequest {
  authorId: number
  body: string
  parentCommentId?: number
}
