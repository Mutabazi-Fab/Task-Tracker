import type { TaskComment } from '../../../types/comment.types'
import { formatDateTime } from '../../../lib/formatDate'
import styles from './TaskLastCommentCell.module.css'

/** Latest comment only — body, author, timestamp. Full history lives on TaskDetailPage. */
export function TaskLastCommentCell({ comment }: { comment: TaskComment | null }) {
  if (!comment) {
    return <span className={styles.empty}>No comments yet</span>
  }

  return (
    <div className={styles.cell}>
      <p className={styles.body}>{comment.body}</p>
      <p className={styles.meta}>
        {comment.authorName} · {formatDateTime(comment.createdAt)}
      </p>
    </div>
  )
}
