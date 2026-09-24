import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { TextField } from '../../../components/ui/TextField'
import styles from './GuidanceNotesSection.module.css'

interface GuidanceNoteFormProps {
  initialTitle?: string
  initialBody?: string
  onSubmit: (title: string, body: string) => void
  onCancel: () => void
  submitting: boolean
  submitLabel: string
}

export function GuidanceNoteForm({ initialTitle = '', initialBody = '', onSubmit, onCancel, submitting, submitLabel }: GuidanceNoteFormProps) {
  const [title, setTitle] = useState(initialTitle)
  const [body, setBody] = useState(initialBody)
  const isValid = title.trim() !== '' && body.trim() !== ''

  return (
    <div className={styles.form}>
      <TextField label="Title" value={title} onChange={setTitle} placeholder="e.g. Who to contact for a Cybersecurity incident" />
      <TextField label="Guidance" value={body} onChange={setBody} placeholder="What a new Director or CEO should know" />
      <div className={styles.formActions}>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button
          type="button"
          variant="primary"
          onClick={() => onSubmit(title.trim(), body.trim())}
          disabled={!isValid || submitting}
        >
          {submitting ? 'Saving…' : submitLabel}
        </Button>
      </div>
    </div>
  )
}
