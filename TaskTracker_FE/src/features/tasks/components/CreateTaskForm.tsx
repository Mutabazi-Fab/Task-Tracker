import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { SegmentedControl } from '../../../components/ui/SegmentedControl'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { usePeople } from '../../people/hooks/usePeople'
import { useTeams } from '../../teams/hooks/useTeams'
import { maxAssignableDate } from '../../../lib/dateLimits'
import type { CreateTaskRequest } from '../../../types/task.types'
import styles from './CreateTaskForm.module.css'

type AssigneeKind = 'TEAM' | 'INDIVIDUAL'

const ASSIGNEE_KIND_OPTIONS: { label: string; value: AssigneeKind }[] = [
  { label: 'Team', value: 'TEAM' },
  { label: 'Individual', value: 'INDIVIDUAL' },
]

interface CreateTaskFormProps {
  onSubmit: (payload: CreateTaskRequest) => void
  onCancel: () => void
  submitting: boolean
}

/**
 * Top-level tasks only — assigned to either a whole team (which a Team Leader/Director
 * later breaks into person-assigned subtasks) or, just as validly, straight to one person
 * (which then behaves like a subtask itself: its % comes directly from comments, never a
 * rollup, and it can never have subtasks of its own — see SubtasksPanel). createdById is
 * always the logged-in Director/Super Admin, not a picker.
 */
export function CreateTaskForm({ onSubmit, onCancel, submitting }: CreateTaskFormProps) {
  const { currentUser } = useAuth()
  const teamsQuery = useTeams()
  const peopleQuery = usePeople()

  const [assigneeKind, setAssigneeKind] = useState<AssigneeKind>('TEAM')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [assignedTeamId, setAssignedTeamId] = useState('')
  const [assignedPersonId, setAssignedPersonId] = useState('')
  const [dateAssigned, setDateAssigned] = useState('')
  const [openingNote, setOpeningNote] = useState('')

  const hasTarget = assigneeKind === 'TEAM' ? assignedTeamId !== '' : assignedPersonId !== ''
  const isValid = title.trim() !== '' && hasTarget && dateAssigned !== '' && openingNote.trim() !== '' && currentUser !== null

  function handleAssigneeKindChange(next: AssigneeKind) {
    setAssigneeKind(next)
    setAssignedTeamId('')
    setAssignedPersonId('')
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return

    onSubmit({
      title: title.trim(),
      description: description.trim() || undefined,
      createdById: currentUser.id,
      assignedTeamId: assigneeKind === 'TEAM' ? Number(assignedTeamId) : undefined,
      assignedPersonId: assigneeKind === 'INDIVIDUAL' ? Number(assignedPersonId) : undefined,
      dateAssigned,
      openingNote: openingNote.trim(),
    })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <TextField label="Title" value={title} onChange={setTitle} placeholder="What needs doing" required />

      <TextField label="Description" value={description} onChange={setDescription} placeholder="Optional detail" />

      <div className={styles.field}>
        <span className={styles.label}>Assign to</span>
        <SegmentedControl options={ASSIGNEE_KIND_OPTIONS} value={assigneeKind} onChange={handleAssigneeKindChange} />
      </div>

      {assigneeKind === 'TEAM' ? (
        <SelectField
          label="Assigned team"
          value={assignedTeamId}
          onChange={setAssignedTeamId}
          placeholder={teamsQuery.isLoading ? 'Loading…' : 'Select a team'}
          options={(teamsQuery.data ?? []).map((team) => ({ label: team.name, value: String(team.id) }))}
        />
      ) : (
        <SelectField
          label="Assigned person"
          value={assignedPersonId}
          onChange={setAssignedPersonId}
          placeholder={peopleQuery.isLoading ? 'Loading…' : 'Select a person'}
          options={(peopleQuery.data ?? []).map((person) => ({ label: person.fullName, value: String(person.id) }))}
        />
      )}

      <TextField
        label="Date assigned"
        type="date"
        value={dateAssigned}
        onChange={setDateAssigned}
        max={maxAssignableDate()}
        required
      />

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
