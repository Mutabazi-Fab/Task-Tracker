import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { EmptyState } from '../../../components/ui/EmptyState'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { useAddDiscussionComment } from '../hooks/useAddDiscussionComment'
import { DiscussionThread } from './DiscussionThread'
import type { TaskComment } from '../../../types/comment.types'
import styles from './DiscussionPanel.module.css'

interface DiscussionPanelProps {
  taskId: number
  /** The task's full comment list (both PROGRESS and DISCUSSION) — filtered down to
   *  DISCUSSION here, so callers don't need to pre-filter. */
  comments: TaskComment[]
}

/**
 * A plain Q&A thread, fully separate from the progress log — anyone who can see this task
 * (no restriction to CEO/Director or any other pair) can ask a question or leave a note
 * here without it touching percentage or status at all. Top-level messages show first,
 * oldest first; each can be replied to, with replies indented underneath — the same shape
 * Instagram uses, and unrelated to "Log progress" above it.
 */
export function DiscussionPanel({ taskId, comments }: DiscussionPanelProps) {
  const [body, setBody] = useState('')

  const { currentUser } = useAuth()
  const addDiscussionComment = useAddDiscussionComment(taskId)

  const discussion = comments.filter((c) => c.type === 'DISCUSSION')
  const topLevel = discussion.filter((c) => c.parentCommentId === null)
  const repliesByParent = new Map<number, TaskComment[]>()
  for (const c of discussion) {
    if (c.parentCommentId !== null) {
      const existing = repliesByParent.get(c.parentCommentId) ?? []
      existing.push(c)
      repliesByParent.set(c.parentCommentId, existing)
    }
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!body.trim() || !currentUser) return

    addDiscussionComment.mutate(
      { authorId: currentUser.id, body: body.trim() },
      { onSuccess: () => setBody('') },
    )
  }

  return (
    <div className={styles.wrap}>
      <form className={styles.form} onSubmit={handleSubmit}>
        <TextField
          label="Add a comment"
          value={body}
          onChange={setBody}
          placeholder="Ask a question or leave a note"
          required
        />
        {addDiscussionComment.isError && <ErrorMessage message={addDiscussionComment.error.message} />}
        <div className={styles.actions}>
          <Button type="submit" disabled={!body.trim() || !currentUser || addDiscussionComment.isPending}>
            {addDiscussionComment.isPending ? 'Posting…' : 'Post comment'}
          </Button>
        </div>
      </form>

      {topLevel.length === 0 ? (
        <EmptyState title="No comments yet" />
      ) : (
        <div>
          {topLevel.map((comment) => (
            <DiscussionThread key={comment.id} taskId={taskId} comment={comment} replies={repliesByParent.get(comment.id) ?? []} />
          ))}
        </div>
      )}
    </div>
  )
}
