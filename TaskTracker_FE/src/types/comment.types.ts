/** One entry in a task's append-only progress log. Immutable — no edit/delete shape exists. */
export interface TaskComment {
  id: number
  sequenceNumber: number
  authorName: string
  percentageAtComment: number
  body: string
  createdAt: string
}

/** Body for POST /tasks/{id}/comments — the only way progress ever changes. */
export interface AddCommentRequest {
  authorId: number
  percentageAtComment: number
  body: string
}
