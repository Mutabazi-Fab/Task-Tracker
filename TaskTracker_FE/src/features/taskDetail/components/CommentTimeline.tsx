import { EmptyState } from '../../../components/ui/EmptyState'
import type { TaskComment } from '../../../types/comment.types'
import { CommentTimelineItem } from './CommentTimelineItem'

/** The progress log specifically — PROGRESS-type comments only, oldest first (as the backend already
 *  orders them). */
export function CommentTimeline({ comments }: { comments: TaskComment[] }) {
  const progress = comments.filter((c) => c.type === 'PROGRESS')

  if (progress.length === 0) {
    return <EmptyState title="No progress logged yet" />
  }

  return (
    <div>
      {progress.map((comment) => (
        <CommentTimelineItem key={comment.id} comment={comment} />
      ))}
    </div>
  )
}
