import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { useAddComment } from '../hooks/useAddComment'
import { ProgressStepButtons } from './ProgressStepButtons'
import styles from './AddCommentForm.module.css'

/**
 * Percentage + body, both required. This is the only place progress can
 * change — there is no path that submits a percentage without the text
 * that explains it. The author is always the logged-in person now — no
 * picker, since we actually know who's logging this.
 */
export function AddCommentForm({ taskId }: { taskId: number }) {
  const [percentage, setPercentage] = useState<number | null>(null)
  const [body, setBody] = useState('')

  const { currentUser } = useAuth()
  const addComment = useAddComment(taskId)

  const isValid = percentage !== null && body.trim() !== '' && currentUser !== null

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || percentage === null || !currentUser) return

    addComment.mutate(
      { authorId: currentUser.id, percentageAtComment: percentage, body: body.trim() },
      {
        onSuccess: () => {
          setBody('')
          setPercentage(null)
        },
      },
    )
  }

  return (
    <Card>
      <form className={styles.form} onSubmit={handleSubmit}>
        <span className={styles.label}>Log progress</span>

        <ProgressStepButtons value={percentage} onChange={setPercentage} />

        <TextField
          label="Exact percentage"
          type="number"
          min={0}
          max={100}
          value={percentage === null ? '' : String(percentage)}
          onChange={(value) => setPercentage(value === '' ? null : Math.min(100, Math.max(0, Number(value))))}
        />

        <TextField
          label="Body"
          value={body}
          onChange={setBody}
          placeholder="Why this percentage — this becomes permanent record"
          required
        />

        {addComment.isError && <ErrorMessage message={addComment.error.message} />}

        <div className={styles.actions}>
          <Button type="submit" disabled={!isValid || addComment.isPending}>
            {addComment.isPending ? 'Logging…' : 'Log progress'}
          </Button>
        </div>
      </form>
    </Card>
  )
}
