import { Avatar } from '../../../components/ui/Avatar'
import { formatDateTime } from '../../../lib/formatDate'
import type { TaskComment } from '../../../types/comment.types'
import styles from './DiscussionCommentItem.module.css'

/** ONE discussion message — author, time, body. */
export function DiscussionCommentItem({ comment }: { comment: TaskComment }) {
  return (
    <div className={styles.item}>
      <Avatar name={comment.authorName} size="sm" />
      <div className={styles.body}>
        <div className={styles.meta}>
          <span className={styles.author}>{comment.authorName}</span>
          <span className={styles.time}>{formatDateTime(comment.createdAt)}</span>
        </div>
        <p className={styles.text}>{comment.body}</p>
      </div>
    </div>
  )
}
