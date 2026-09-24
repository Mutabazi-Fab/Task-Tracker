import { useEffect, useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { Modal } from '../../../components/ui/Modal'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { useAddComment } from '../hooks/useAddComment'
import { ProgressSlider } from './ProgressSlider'
import styles from './AddCommentForm.module.css'

interface AddCommentFormProps {
  taskId: number
  /** Seeds the slider's starting position. Once already 100, the form retires — see below. */
  currentPercentage: number
}

/** The progress log — only rendered for an individually-tracked task (a TEAM/DEPARTMENT task's
 *  percentage is always a rollup, never set here). */
export function AddCommentForm({ taskId, currentPercentage }: AddCommentFormProps) {
  const [percentage, setPercentage] = useState(currentPercentage)
  const [body, setBody] = useState('')
  const [confirmCompleteOpen, setConfirmCompleteOpen] = useState(false)

  const { currentUser } = useAuth()
  const addComment = useAddComment(taskId)

  // Follow the task's real progress: after a log succeeds the task refetches and this prop
  // becomes the value just logged, so the slider stays there instead of jumping back.
  useEffect(() => {
    setPercentage(currentPercentage)
  }, [currentPercentage])

  const isValid = body.trim() !== '' && currentUser !== null

  function submit() {
    if (!isValid || !currentUser) return

    addComment.mutate(
      { authorId: currentUser.id, percentageAtComment: percentage, body: body.trim() },
      {
        onSuccess: () => {
          // Deliberately NOT resetting percentage here: currentPercentage is still the
          // pre-log value at this point (the task hasn't refetched yet), so resetting to it
          // is what used to snap the slider back to 0 after every log.
          setBody('')
          setConfirmCompleteOpen(false)
        },
      },
    )
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid) return

    if (percentage === 100) {
      setConfirmCompleteOpen(true)
      return
    }
    submit()
  }

  if (currentPercentage === 100) {
    return (
      <Card>
        <span className={styles.label}>Log progress</span>
        <p className={styles.doneNote}>This task is already at 100% — there's nothing left to log.</p>
      </Card>
    )
  }

  return (
    <Card>
      <form className={styles.form} onSubmit={handleSubmit}>
        <span className={styles.label}>Log progress</span>

        <ProgressSlider value={percentage} onChange={setPercentage} />

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

      <Modal open={confirmCompleteOpen} onClose={() => setConfirmCompleteOpen(false)} title="Mark this task as done?">
        <p className={styles.confirmText}>
          You're about to log progress at 100% — that marks this task complete and retires the progress log for
          good. Are you sure you're done with this task?
        </p>
        {addComment.isError && <ErrorMessage message={addComment.error.message} />}
        <div className={styles.actions}>
          <Button type="button" variant="ghost" onClick={() => setConfirmCompleteOpen(false)} disabled={addComment.isPending}>
            No, not yet
          </Button>
          <Button type="button" onClick={submit} disabled={addComment.isPending}>
            {addComment.isPending ? 'Logging…' : 'Yes, mark it done'}
          </Button>
        </div>
      </Modal>
    </Card>
  )
}
