import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { useTeams } from '../../teams/hooks/useTeams'
import type { CreateTaskRequest } from '../../../types/task.types'
import styles from './CreateTaskForm.module.css'

interface CreateTaskFormProps {
  onSubmit: (payload: CreateTaskRequest) => void
  onCancel: () => void
  submitting: boolean
}

/**
 * Top-level tasks only — always assigned to a team, never an individual (that's a
 * structural rule now, not a free choice; see CreateSubtaskForm for assigning to a
 * person under a top-level task). createdById is always the logged-in Director/Super
 * Admin, not a picker.
 */
export function CreateTaskForm({ onSubmit, onCancel, submitting }: CreateTaskFormProps) {
  const { currentUser } = useAuth()
  const teamsQuery = useTeams()

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [assignedTeamId, setAssignedTeamId] = useState('')
  const [dateAssigned, setDateAssigned] = useState('')
  const [openingNote, setOpeningNote] = useState('')

  const isValid =
    title.trim() !== '' &&
    assignedTeamId !== '' &&
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
      assignedTeamId: Number(assignedTeamId),
      dateAssigned,
      openingNote: openingNote.trim(),
    })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <TextField label="Title" value={title} onChange={setTitle} placeholder="What needs doing" required />

      <TextField label="Description" value={description} onChange={setDescription} placeholder="Optional detail" />

      <SelectField
        label="Assigned team"
        value={assignedTeamId}
        onChange={setAssignedTeamId}
        placeholder={teamsQuery.isLoading ? 'Loading…' : 'Select a team'}
        options={(teamsQuery.data ?? []).map((team) => ({ label: team.name, value: String(team.id) }))}
      />

      <TextField label="Date assigned" type="date" value={dateAssigned} onChange={setDateAssigned} required />

      <TextField
        label="Opening note"
        value={openingNote}
        onChange={setOpeningNote}
        placeholder="Why this task starts at 0%"
        required
      />

      <div className={styles.actions}>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" variant="primary" disabled={!isValid || submitting}>
          {submitting ? 'Creating…' : 'Create task'}
        </Button>
      </div>
    </form>
  )
}
