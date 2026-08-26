import { Avatar } from '../../../components/ui/Avatar'
import { Icon } from '../../../components/ui/Icon'
import { formatDateTime } from '../../../lib/formatDate'
import { formatPercentage } from '../../../lib/formatPercentage'
import type { TaskComment } from '../../../types/comment.types'
import styles from './CommentTimelineItem.module.css'

/** ONE comment: #n, author, time, %, body. Immutable — no edit/delete affordance. */
export function CommentTimelineItem({ comment }: { comment: TaskComment }) {
  return (
    <div className={styles.item}>
      <Avatar name={comment.authorName} size="sm" />
      <div className={styles.body}>
        <div className={styles.meta}>
          <span className={styles.checkBadge}>
            <Icon name="check" size={11} />
          </span>
          <span className={styles.sequence}>#{comment.sequenceNumber}</span>
          <span className={styles.author}>{comment.authorName}</span>
          <span className={styles.percentage}>{formatPercentage(comment.percentageAtComment)}</span>
          <span className={styles.time}>{formatDateTime(comment.createdAt)}</span>
        </div>
        <p className={styles.text}>{comment.body}</p>
      </div>
    </div>
  )
}
