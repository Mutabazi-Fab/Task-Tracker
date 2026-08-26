import { EmptyState } from '../../../components/ui/EmptyState'
import type { TaskComment } from '../../../types/comment.types'
import { CommentTimelineItem } from './CommentTimelineItem'

/** Maps the full history, oldest first. The backend already orders it this way. */
export function CommentTimeline({ comments }: { comments: TaskComment[] }) {
  if (comments.length === 0) {
    return <EmptyState title="No comments yet" />
  }

  return (
    <div>
      {comments.map((comment) => (
        <CommentTimelineItem key={comment.id} comment={comment} />
      ))}
    </div>
  )
}
