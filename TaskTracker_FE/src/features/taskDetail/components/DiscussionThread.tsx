import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { useAddDiscussionComment } from '../hooks/useAddDiscussionComment'
import { DiscussionCommentItem } from './DiscussionCommentItem'
import type { TaskComment } from '../../../types/comment.types'
import styles from './DiscussionThread.module.css'

interface DiscussionThreadProps {
  taskId: number
  comment: TaskComment
  replies: TaskComment[]
}

/**
 * One top-level discussion message, its replies (indented underneath, oldest first), and
 * a "Reply" toggle that reveals an inline reply box — the same shape Instagram uses.
 * Replying always threads under THIS top-level comment, even when replying to one of its
 * replies (the backend flattens that automatically), so there's never more than one level
 * of nesting to render.
 */
export function DiscussionThread({ taskId, comment, replies }: DiscussionThreadProps) {
  const [replyOpen, setReplyOpen] = useState(false)
  const [replyBody, setReplyBody] = useState('')

  const { currentUser } = useAuth()
  const addDiscussionComment = useAddDiscussionComment(taskId)

  function handleReply(e: React.FormEvent) {
    e.preventDefault()
    if (!replyBody.trim() || !currentUser) return

    addDiscussionComment.mutate(
      { authorId: currentUser.id, body: replyBody.trim(), parentCommentId: comment.id },
      {
        onSuccess: () => {
          setReplyBody('')
          setReplyOpen(false)
        },
      },
    )
  }

  return (
    <div className={styles.thread}>
      <DiscussionCommentItem comment={comment} />

      <button type="button" className={styles.replyToggle} onClick={() => setReplyOpen((open) => !open)}>
        {replyOpen ? 'Cancel' : 'Reply'}
      </button>

      {replies.length > 0 && (
        <div className={styles.replies}>
          {replies.map((reply) => (
            <DiscussionCommentItem key={reply.id} comment={reply} />
          ))}
        </div>
      )}

      {replyOpen && (
        <form className={styles.replyForm} onSubmit={handleReply}>
          <TextField
            value={replyBody}
            onChange={setReplyBody}
            placeholder={`Reply to ${comment.authorName}`}
            required
          />
          {addDiscussionComment.isError && <ErrorMessage message={addDiscussionComment.error.message} />}
          <Button type="submit" disabled={!replyBody.trim() || addDiscussionComment.isPending}>
            {addDiscussionComment.isPending ? 'Posting…' : 'Reply'}
          </Button>
        </form>
      )}
    </div>
  )
}
