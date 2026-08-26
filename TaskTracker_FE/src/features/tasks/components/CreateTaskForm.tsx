import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { SegmentedControl } from '../../../components/ui/SegmentedControl'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import type { AssigneeType } from '../../../types/task.types'
import type { CreateTaskRequest } from '../../../types/task.types'
import { usePeople } from '../../people/hooks/usePeople'
import { useTeams } from '../../teams/hooks/useTeams'
import styles from './CreateTaskForm.module.css'

interface CreateTaskFormProps {
  onSubmit: (payload: CreateTaskRequest) => void
  onCancel: () => void
  submitting: boolean
}

const ASSIGNEE_TYPE_OPTIONS: { label: string; value: AssigneeType }[] = [
  { label: 'Individual', value: 'INDIVIDUAL' },
  { label: 'Team', value: 'TEAM' },
]

/** The fields only — CreateTaskModal owns the mutation and the open/close state. */
export function CreateTaskForm({ onSubmit, onCancel, submitting }: CreateTaskFormProps) {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [assignedById, setAssignedById] = useState('')
  const [assigneeType, setAssigneeType] = useState<AssigneeType>('INDIVIDUAL')
  const [assignedPersonId, setAssignedPersonId] = useState('')
  const [assignedTeamId, setAssignedTeamId] = useState('')
  const [dateAssigned, setDateAssigned] = useState('')
  const [openingNote, setOpeningNote] = useState('')

  const peopleQuery = usePeople()
  const teamQuery = useTeams()

  const hasAssignee = assigneeType === 'INDIVIDUAL' ? assignedPersonId !== '' : assignedTeamId !== ''
  const isValid = title.trim() !== '' && assignedById !== '' && dateAssigned !== '' && openingNote.trim() !== '' && hasAssignee

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid) return

    onSubmit({
      title: title.trim(),
      description: description.trim() || undefined,
      assignedById: Number(assignedById),
      assigneeType,
      assignedPersonId: assigneeType === 'INDIVIDUAL' ? Number(assignedPersonId) : undefined,
      assignedTeamId: assigneeType === 'TEAM' ? Number(assignedTeamId) : undefined,
      dateAssigned,
      openingNote: openingNote.trim(),
    })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <TextField label="Title" value={title} onChange={setTitle} placeholder="What needs doing" required />

      <TextField label="Description" value={description} onChange={setDescription} placeholder="Optional detail" />

      <SelectField
        label="Assigned by"
        value={assignedById}
        onChange={setAssignedById}
        placeholder={peopleQuery.isLoading ? 'Loading…' : 'Select a person'}
        options={(peopleQuery.data ?? []).map((person) => ({ label: person.fullName, value: String(person.id) }))}
      />

      <div className={styles.field}>
        <span className={styles.label}>Assignee type</span>
        <SegmentedControl options={ASSIGNEE_TYPE_OPTIONS} value={assigneeType} onChange={setAssigneeType} />
      </div>

      {assigneeType === 'INDIVIDUAL' ? (
        <SelectField
          label="Assigned to"
          value={assignedPersonId}
          onChange={setAssignedPersonId}
          placeholder={peopleQuery.isLoading ? 'Loading…' : 'Select a person'}
          options={(peopleQuery.data ?? []).map((person) => ({ label: person.fullName, value: String(person.id) }))}
        />
      ) : (
        <SelectField
          label="Assigned to"
          value={assignedTeamId}
          onChange={setAssignedTeamId}
          placeholder={teamQuery.isLoading ? 'Loading…' : 'Select a team'}
          options={(teamQuery.data ?? []).map((team) => ({ label: team.name, value: String(team.id) }))}
        />
      )}

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
