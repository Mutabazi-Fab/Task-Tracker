import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { useAddComment } from '../hooks/useAddComment'
import { ProgressStepButtons } from './ProgressStepButtons'
import styles from './AddCommentForm.module.css'

interface AddCommentFormProps {
  taskId: number
  /** A team-assigned task's percentage is always the average of its subtasks — never
   *  something typed here. When true, this drops the percentage picker entirely and
   *  becomes a plain narrative note; showing a picker that silently does nothing to the
   *  task's actual progress would just be misleading. The backend independently ignores
   *  whatever percentage is sent for a team-assigned task too — this isn't the only guard,
   *  just what keeps the form honest about what it's actually doing. */
  isTeamAssigned: boolean
}

export function AddCommentForm({ taskId, isTeamAssigned }: AddCommentFormProps) {
  const [percentage, setPercentage] = useState<number | null>(null)
  const [body, setBody] = useState('')

  const { currentUser } = useAuth()
  const addComment = useAddComment(taskId)

  const isValid = (isTeamAssigned || percentage !== null) && body.trim() !== '' && currentUser !== null

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return
    if (!isTeamAssigned && percentage === null) return

    addComment.mutate(
      // 0 for a team-assigned task is a placeholder only — the backend stamps the
      // comment's real percentage from the task's own current rollup instead of trusting
      // this field, precisely because there's no meaningful number to send here.
      { authorId: currentUser.id, percentageAtComment: isTeamAssigned ? 0 : (percentage as number), body: body.trim() },
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
        <span className={styles.label}>{isTeamAssigned ? 'Add a note' : 'Log progress'}</span>

        {isTeamAssigned ? (
          <p className={styles.hint}>
            This task's progress is the average of its subtasks — a note here explains what changed, it doesn't set a
            percentage.
          </p>
        ) : (
          <>
            <ProgressStepButtons value={percentage} onChange={setPercentage} />

            <TextField
              label="Exact percentage"
              type="number"
              min={0}
              max={100}
              value={percentage === null ? '' : String(percentage)}
              onChange={(value) => setPercentage(value === '' ? null : Math.min(100, Math.max(0, Number(value))))}
            />
          </>
        )}

        <TextField
          label="Body"
          value={body}
          onChange={setBody}
          placeholder={isTeamAssigned ? "What's changed" : 'Why this percentage — this becomes permanent record'}
          required
        />

        {addComment.isError && <ErrorMessage message={addComment.error.message} />}

        <div className={styles.actions}>
          <Button type="submit" disabled={!isValid || addComment.isPending}>
            {addComment.isPending ? 'Logging…' : isTeamAssigned ? 'Add note' : 'Log progress'}
          </Button>
        </div>
      </form>
    </Card>
  )
}
