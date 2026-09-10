import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { SegmentedControl } from '../../../components/ui/SegmentedControl'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { usePeople } from '../../people/hooks/usePeople'
import { useTeams } from '../../teams/hooks/useTeams'
import { useDepartments } from '../../departments/hooks/useDepartments'
import { maxAssignableDate } from '../../../lib/dateLimits'
import type { CreateTaskRequest, TaskSeverity, TaskSource } from '../../../types/task.types'
import styles from './CreateTaskForm.module.css'

type AssigneeKind = 'TEAM' | 'INDIVIDUAL' | 'DEPARTMENT'

const ASSIGNEE_KIND_OPTIONS: { label: string; value: AssigneeKind }[] = [
  { label: 'Team', value: 'TEAM' },
  { label: 'Individual', value: 'INDIVIDUAL' },
]

// Executive/Super Admin only — appended to the options above when the caller qualifies,
// rather than baked into the constant list, so a plain Director never sees a choice
// that'd just be rejected server-side.
const DEPARTMENT_OPTION: { label: string; value: AssigneeKind } = { label: 'Department', value: 'DEPARTMENT' }

const SOURCE_OPTIONS: { label: string; value: TaskSource }[] = [
  { label: 'Initiative', value: 'INITIATIVE' },
  { label: 'Auditor', value: 'AUDITOR' },
  { label: 'Regulator', value: 'REGULATOR' },
  { label: 'Board', value: 'BOARD' },
]

// Executive/Super Admin only — the backend rejects a non-Executive creator's attempt to
// set severity, so a plain Director never sees a picker that'd just be rejected.
const SEVERITY_OPTIONS: { label: string; value: TaskSeverity }[] = [
  { label: 'Low', value: 'LOW' },
  { label: 'Medium', value: 'MEDIUM' },
  { label: 'High', value: 'HIGH' },
  { label: 'Critical', value: 'CRITICAL' },
]

interface CreateTaskFormProps {
  onSubmit: (payload: CreateTaskRequest) => void
  onCancel: () => void
  submitting: boolean
}

/**
 * Top-level (depth 0) tasks only — assigned to a whole team (which a Team Leader/Director
 * later breaks into person-assigned subtasks), straight to one person (which then behaves
 * like a subtask itself: its % comes directly from comments, never a rollup, and it can
 * never have subtasks of its own — see SubtasksPanel), or — Executive/Super Admin only —
 * a whole Department (whose head Director then turns it into a real team-or-individual
 * "implementation task", one level deeper, via the same "Add subtask" flow). createdById
 * is always the logged-in Director-or-above, not a picker.
 */
export function CreateTaskForm({ onSubmit, onCancel, submitting }: CreateTaskFormProps) {
  const { currentUser, isExecutive } = useAuth()
  const teamsQuery = useTeams()
  const peopleQuery = usePeople()
  const departmentsQuery = useDepartments()

  const assigneeKindOptions = isExecutive ? [...ASSIGNEE_KIND_OPTIONS, DEPARTMENT_OPTION] : ASSIGNEE_KIND_OPTIONS

  const [assigneeKind, setAssigneeKind] = useState<AssigneeKind>('TEAM')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [assignedTeamId, setAssignedTeamId] = useState('')
  const [assignedPersonId, setAssignedPersonId] = useState('')
  const [assignedDepartmentId, setAssignedDepartmentId] = useState('')
  const [dateAssigned, setDateAssigned] = useState('')
  const [deadline, setDeadline] = useState('')
  const [source, setSource] = useState<TaskSource | ''>('')
  const [sourceLabel, setSourceLabel] = useState('')
  const [severity, setSeverity] = useState<TaskSeverity | ''>('')
  const [openingNote, setOpeningNote] = useState('')

  const hasTarget =
    assigneeKind === 'TEAM' ? assignedTeamId !== ''
    : assigneeKind === 'INDIVIDUAL' ? assignedPersonId !== ''
    : assignedDepartmentId !== ''
  const isDeadlineBeforeAssignment = dateAssigned !== '' && deadline !== '' && deadline < dateAssigned
  const isValid =
    title.trim() !== '' &&
    hasTarget &&
    dateAssigned !== '' &&
    deadline !== '' &&
    !isDeadlineBeforeAssignment &&
    openingNote.trim() !== '' &&
    currentUser !== null

  function handleAssigneeKindChange(next: AssigneeKind) {
    setAssigneeKind(next)
    setAssignedTeamId('')
    setAssignedPersonId('')
    setAssignedDepartmentId('')
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
      assignedDepartmentId: assigneeKind === 'DEPARTMENT' ? Number(assignedDepartmentId) : undefined,
      dateAssigned,
      deadline,
      source: source || undefined,
      sourceLabel: source && sourceLabel.trim() ? sourceLabel.trim() : undefined,
      severity: isExecutive && severity ? severity : undefined,
      openingNote: openingNote.trim(),
    })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <TextField label="Title" value={title} onChange={setTitle} placeholder="What needs doing" required />

      <TextField label="Description" value={description} onChange={setDescription} placeholder="Optional detail" />

      <div className={styles.field}>
        <span className={styles.label}>Assign to</span>
        <SegmentedControl options={assigneeKindOptions} value={assigneeKind} onChange={handleAssigneeKindChange} />
      </div>

      {assigneeKind === 'TEAM' && (
        <SelectField
          label="Assigned team"
          value={assignedTeamId}
          onChange={setAssignedTeamId}
          placeholder={teamsQuery.isLoading ? 'Loading…' : 'Select a team'}
          options={(teamsQuery.data ?? []).map((team) => ({ label: team.name, value: String(team.id) }))}
        />
      )}
      {assigneeKind === 'INDIVIDUAL' && (
        <SelectField
          label="Assigned person"
          value={assignedPersonId}
          onChange={setAssignedPersonId}
          placeholder={peopleQuery.isLoading ? 'Loading…' : 'Select a person'}
          options={(peopleQuery.data ?? []).map((person) => ({ label: person.fullName, value: String(person.id) }))}
        />
      )}
      {assigneeKind === 'DEPARTMENT' && (
        <SelectField
          label="Assigned department"
          value={assignedDepartmentId}
          onChange={setAssignedDepartmentId}
          placeholder={departmentsQuery.isLoading ? 'Loading…' : 'Select a department'}
          options={(departmentsQuery.data ?? []).map((d) => ({ label: d.name, value: String(d.id) }))}
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
        label="Deadline"
        type="date"
        value={deadline}
        onChange={setDeadline}
        min={dateAssigned || undefined}
        required
      />
      {isDeadlineBeforeAssignment && <ErrorMessage message="Deadline can't be before the date assigned." />}

      <SelectField
        label="Source (optional)"
        value={source}
        onChange={(v) => setSource(v as TaskSource)}
        placeholder="Where this came from"
        options={SOURCE_OPTIONS.map((o) => ({ label: o.label, value: o.value }))}
      />
      {source && (
        <TextField
          label="Source detail"
          value={sourceLabel}
          onChange={setSourceLabel}
          placeholder="e.g. Director Musoni, GPO, E&Y, Board of Directors"
        />
      )}

      {isExecutive && (
        <SelectField
          label="Severity (optional)"
          value={severity}
          onChange={(v) => setSeverity(v as TaskSeverity)}
          placeholder="Not classified"
          options={SEVERITY_OPTIONS.map((o) => ({ label: o.label, value: o.value }))}
        />
      )}

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
