import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAddComment } from '../hooks/useAddComment'
import { usePeople } from '../../people/hooks/usePeople'
import { ProgressStepButtons } from './ProgressStepButtons'
import styles from './AddCommentForm.module.css'

/**
 * Percentage + body, both required. This is the only place progress can
 * change — there is no path that submits a percentage without the text
 * that explains it.
 */
export function AddCommentForm({ taskId }: { taskId: number }) {
  const [percentage, setPercentage] = useState<number | null>(null)
  const [body, setBody] = useState('')
  const [authorId, setAuthorId] = useState('')

  const peopleQuery = usePeople()
  const addComment = useAddComment(taskId)

  const isValid = percentage !== null && body.trim() !== '' && authorId !== ''

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || percentage === null) return

    addComment.mutate(
      { authorId: Number(authorId), percentageAtComment: percentage, body: body.trim() },
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

        <SelectField
          label="Author"
          value={authorId}
          onChange={setAuthorId}
          placeholder={peopleQuery.isLoading ? 'Loading…' : 'Who is logging this'}
          options={(peopleQuery.data ?? []).map((person) => ({ label: person.fullName, value: String(person.id) }))}
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
