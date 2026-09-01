import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { useTeamMembers } from '../../teams/hooks/useTeamMembers'
import type { CreateSubtaskRequest } from '../../../types/task.types'
import styles from '../../tasks/components/CreateTaskForm.module.css'

interface CreateSubtaskFormProps {
  /** The parent (top-level) task's assigned team — subtask assignees are scoped to its
   *  members, never anyone outside the team. */
  teamId: number
  onSubmit: (payload: CreateSubtaskRequest) => void
  onCancel: () => void
  submitting: boolean
}

/** createdById is always the logged-in person — the backend still checks they're either
 *  a Director/Super Admin or this team's leader, but there's no reason to ask when we
 *  already know who's here. */
export function CreateSubtaskForm({ teamId, onSubmit, onCancel, submitting }: CreateSubtaskFormProps) {
  const { currentUser } = useAuth()
  const membersQuery = useTeamMembers(teamId)

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [assignedPersonId, setAssignedPersonId] = useState('')
  const [dateAssigned, setDateAssigned] = useState('')
  const [openingNote, setOpeningNote] = useState('')

  const isValid =
    title.trim() !== '' &&
    assignedPersonId !== '' &&
    dateAssigned !== '' &&
    openingNote.trim() !== '' &&
    currentUser !== null

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return

    onSubmit({
      title: title.trim(),
      description: description.trim() || undefined,
      createdById: currentUser.id,
      assignedPersonId: Number(assignedPersonId),
      dateAssigned,
      openingNote: openingNote.trim(),
    })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <TextField label="Title" value={title} onChange={setTitle} placeholder="What needs doing" required />

      <TextField label="Description" value={description} onChange={setDescription} placeholder="Optional detail" />

      <SelectField
        label="Assigned to"
        value={assignedPersonId}
        onChange={setAssignedPersonId}
        placeholder={membersQuery.isLoading ? 'Loading…' : 'Select a team member'}
        options={(membersQuery.data ?? []).map((member) => ({ label: member.fullName, value: String(member.personId) }))}
      />

      <TextField label="Date assigned" type="date" value={dateAssigned} onChange={setDateAssigned} required />

      <TextField
        label="Opening note"
        value={openingNote}
        onChange={setOpeningNote}
        placeholder="Why this subtask starts at 0%"
        required
      />

      <div className={styles.actions}>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" variant="primary" disabled={!isValid || submitting}>
          {submitting ? 'Creating…' : 'Create subtask'}
        </Button>
      </div>
    </form>
  )
}
